package com.circuitcraft.app.data;

import android.util.Log;

import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ServerValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;


public class ScoreRepository {
    private static final String TAG = "ScoreRepository";

    public static String displayName(FirebaseUser user) {
        String n = user.getDisplayName();
        if (n != null && !n.isEmpty()) return n;
        String email = user.getEmail();
        return email != null ? email : "Student";
    }

    public static void save(FirebaseUser user, int score, int total, int seconds) {
        final String name = displayName(user);

        // 1) Firestore: full history
        Map<String, Object> attempt = new HashMap<>();
        attempt.put("uid", user.getUid());
        attempt.put("email", user.getEmail());
        attempt.put("score", score);
        attempt.put("total", total);
        attempt.put("seconds", seconds);
        attempt.put("timestamp", System.currentTimeMillis());
        FirebaseFirestore.getInstance().collection("scores").add(attempt);

        // 2) Realtime Database: keep only the best result of this user
        try {
            final DatabaseReference ref = RealtimeDb.get().getReference("leaderboard").child(user.getUid());
            ref.get().addOnCompleteListener(task -> {
                if (!task.isSuccessful()) {
                    Log.w(TAG, "Realtime DB read failed: " + task.getException());
                    return;
                }
                long oldScore = -1, oldSeconds = Long.MAX_VALUE;
                if (task.getResult() != null && task.getResult().exists()) {
                    Long s = task.getResult().child("score").getValue(Long.class);
                    Long t = task.getResult().child("seconds").getValue(Long.class);
                    if (s != null) oldScore = s;
                    if (t != null) oldSeconds = t;
                }
                if (score > oldScore || (score == oldScore && seconds < oldSeconds)) {
                    Map<String, Object> best = new HashMap<>();
                    best.put("name", name);
                    best.put("score", score);
                    best.put("total", total);
                    best.put("seconds", seconds);
                    best.put("updatedAt", ServerValue.TIMESTAMP);
                    ref.setValue(best);
                }
            });
        } catch (Exception e) {
            Log.w(TAG, "Realtime Database not configured: " + e.getMessage());
        }
    }
}
