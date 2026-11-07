package com.circuitcraft.app.ui;

import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.circuitcraft.app.databinding.ItemLeaderboardBinding;
import com.circuitcraft.app.model.LeaderboardEntry;

import java.util.List;
import java.util.Locale;

public class LeaderboardAdapter extends RecyclerView.Adapter<LeaderboardAdapter.VH> {
    private final List<LeaderboardEntry> items;
    private final String myUid;

    public LeaderboardAdapter(List<LeaderboardEntry> items, String myUid) {
        this.items = items;
        this.myUid = myUid;
    }

    @NonNull
    @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(ItemLeaderboardBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int position) {
        LeaderboardEntry e = items.get(position);
        String[] medals = {"🥇", "🥈", "🥉"};
        h.b.tvRank.setText(position < 3 ? medals[position] : String.valueOf(position + 1));
        boolean me = e.uid != null && e.uid.equals(myUid);
        h.b.tvName.setText((e.name == null ? "Student" : e.name) + (me ? "  (you)" : ""));
        h.b.tvTime.setText(String.format(Locale.US, "Time %02d:%02d", e.seconds / 60, e.seconds % 60));
        h.b.tvScore.setText(e.score + " / " + e.total);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class VH extends RecyclerView.ViewHolder {
        final ItemLeaderboardBinding b;

        VH(ItemLeaderboardBinding b) {
            super(b.getRoot());
            this.b = b;
        }
    }
}
