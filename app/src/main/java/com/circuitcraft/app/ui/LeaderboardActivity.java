package com.circuitcraft.app.ui;

import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.circuitcraft.app.data.RealtimeDb;
import com.circuitcraft.app.databinding.ActivityLeaderboardBinding;
import com.circuitcraft.app.model.LeaderboardEntry;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Top-20 leaderboard read from Firebase REALTIME DATABASE with a ValueEventListener:
 * the list updates by itself the moment anybody finishes a quiz - no refresh needed.
 */
public class LeaderboardActivity extends BaseActivity {
    private ActivityLeaderboardBinding b;
    private final List<LeaderboardEntry> items = new ArrayList<>();
    private LeaderboardAdapter adapter;
    private Query query;
    private ValueEventListener listener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        b = ActivityLeaderboardBinding.inflate(getLayoutInflater());
        setContentView(b.getRoot());

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        adapter = new LeaderboardAdapter(items, user != null ? user.getUid() : "");
        b.rvLeaderboard.setLayoutManager(new LinearLayoutManager(this));
        b.rvLeaderboard.setAdapter(adapter);
    }

    @Override
    protected void onStart() {
        super.onStart();
        try {
            query = RealtimeDb.get().getReference("leaderboard").orderByChild("score").limitToLast(20);
            listener = new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    items.clear();
                    for (DataSnapshot c : snapshot.getChildren()) {
                        LeaderboardEntry e = new LeaderboardEntry();
                        e.uid = c.getKey();
                        e.name = c.child("name").getValue(String.class);
                        Long s = c.child("score").getValue(Long.class);
                        Long t = c.child("total").getValue(Long.class);
                        Long sec = c.child("seconds").getValue(Long.class);
                        e.score = s == null ? 0 : s.intValue();
                        e.total = t == null ? 0 : t.intValue();
                        e.seconds = sec == null ? 0 : sec.intValue();
                        items.add(e);
                    }
                    // highest score first, then fastest time
                    Collections.sort(items, (x, y) -> x.score != y.score
                            ? Integer.compare(y.score, x.score) : Integer.compare(x.seconds, y.seconds));
                    adapter.notifyDataSetChanged();
                    b.tvEmpty.setVisibility(items.isEmpty() ? View.VISIBLE : View.GONE);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(LeaderboardActivity.this,
                            "Realtime Database error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                }
            };
            query.addValueEventListener(listener);
        } catch (Exception ex) {
            Toast.makeText(this, "Realtime Database is not set up yet: " + ex.getMessage(), Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (query != null && listener != null) query.removeEventListener(listener);   // stop listening when hidden
    }
}
