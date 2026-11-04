package com.circuitcraft.app.data;

import com.google.firebase.database.FirebaseDatabase;


public class RealtimeDb {
    public static final String DB_URL = "";

    public static FirebaseDatabase get() {
        return DB_URL.isEmpty() ? FirebaseDatabase.getInstance() : FirebaseDatabase.getInstance(DB_URL);
    }
}
