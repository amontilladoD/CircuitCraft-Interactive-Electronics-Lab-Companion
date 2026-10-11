package com.circuitcraft.app.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class FavouriteProvider extends ContentProvider {
    public static final String AUTHORITY = "com.circuitcraft.app.provider";
    public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY + "/favourites");

    private static final int FAVOURITES = 1;
    private static final int FAVOURITE_ID = 2;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);

    static {
        MATCHER.addURI(AUTHORITY, "favourites", FAVOURITES);
        MATCHER.addURI(AUTHORITY, "favourites/#", FAVOURITE_ID);
    }

    private DbHelper helper;

    @Override
    public boolean onCreate() {
        helper = new DbHelper(getContext());
        return true;
    }

    private String withId(String selection, Uri uri) {
        String idClause = DbHelper.COL_ID + "=" + ContentUris.parseId(uri);
        return TextUtils.isEmpty(selection) ? idClause : idClause + " AND (" + selection + ")";
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        if (MATCHER.match(uri) == FAVOURITE_ID) selection = withId(selection, uri);
        SQLiteDatabase db = helper.getReadableDatabase();
        Cursor c = db.query(DbHelper.TABLE, projection, selection, selectionArgs, null, null, sortOrder);
        c.setNotificationUri(getContext().getContentResolver(), uri);
        return c;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        return MATCHER.match(uri) == FAVOURITE_ID
                ? "vnd.android.cursor.item/vnd." + AUTHORITY + ".favourite"
                : "vnd.android.cursor.dir/vnd." + AUTHORITY + ".favourite";
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        long id = helper.getWritableDatabase().insert(DbHelper.TABLE, null, values);
        if (id < 0) return null;
        getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return ContentUris.withAppendedId(CONTENT_URI, id);
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        if (MATCHER.match(uri) == FAVOURITE_ID) selection = withId(selection, uri);
        int n = helper.getWritableDatabase().delete(DbHelper.TABLE, selection, selectionArgs);
        if (n > 0) getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return n;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        if (MATCHER.match(uri) == FAVOURITE_ID) selection = withId(selection, uri);
        int n = helper.getWritableDatabase().update(DbHelper.TABLE, values, selection, selectionArgs);
        if (n > 0) getContext().getContentResolver().notifyChange(CONTENT_URI, null);
        return n;
    }
}
