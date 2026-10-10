package com.circuitcraft.app.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DbHelper extends SQLiteOpenHelper {
    public static final String DB_NAME = "circuitcraft.db";
    public static final int DB_VERSION = 2;

    public static final String TABLE = "favourites";
    public static final String COL_ID = "_id";
    public static final String COL_OWNER = "owner";
    public static final String COL_NAME = "name";
    public static final String COL_DATA = "data";        
    public static final String COL_SUMMARY = "summary";  

    public DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE + " ("
                + COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + COL_OWNER + " TEXT NOT NULL, "
                + COL_NAME + " TEXT NOT NULL, "
                + COL_DATA + " TEXT NOT NULL, "
                + COL_SUMMARY + " TEXT)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE);
        onCreate(db);
    }
}
