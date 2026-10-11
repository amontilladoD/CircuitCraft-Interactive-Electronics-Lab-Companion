package com.circuitcraft.app.data;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;

import com.circuitcraft.app.model.Favourite;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class FavouriteRepository {

    public static long save(Context ctx, Favourite f) {
        ContentValues v = new ContentValues();
        v.put(DbHelper.COL_OWNER, f.owner);
        v.put(DbHelper.COL_NAME, f.name);
        v.put(DbHelper.COL_DATA, f.data);
        v.put(DbHelper.COL_SUMMARY, f.summary);

        Uri uri = ctx.getContentResolver().insert(FavouriteProvider.CONTENT_URI, v);
        if (uri == null) return -1;
        f.id = ContentUris.parseId(uri);

        FirebaseFirestore.getInstance().collection("users").document(f.owner)
                .collection("favourites").document(String.valueOf(f.id)).set(f.toMap());
        return f.id;
    }

    public static void delete(Context ctx, Favourite f) {
        Uri uri = ContentUris.withAppendedId(FavouriteProvider.CONTENT_URI, f.id);
        ctx.getContentResolver().delete(uri, null, null);
        FirebaseFirestore.getInstance().collection("users").document(f.owner)
                .collection("favourites").document(String.valueOf(f.id)).delete();
    }

    public static List<Favourite> loadAll(Context ctx, String owner) {
        List<Favourite> list = new ArrayList<>();
        ContentResolver cr = ctx.getContentResolver();
        Cursor c = cr.query(FavouriteProvider.CONTENT_URI, null,
                DbHelper.COL_OWNER + "=?", new String[]{owner}, DbHelper.COL_ID + " DESC");
        if (c == null) return list;
        try {
            while (c.moveToNext()) {
                Favourite f = new Favourite();
                f.id = c.getLong(c.getColumnIndexOrThrow(DbHelper.COL_ID));
                f.owner = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_OWNER));
                f.name = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_NAME));
                f.data = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_DATA));
                f.summary = c.getString(c.getColumnIndexOrThrow(DbHelper.COL_SUMMARY));
                list.add(f);
            }
        } finally {
            c.close();
        }
        return list;
    }
}
