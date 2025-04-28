package com.example.water_app;

import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.example.water_app.R;

import java.util.List;

public class RankingAdapter {

    public static class RankingEntry {
        String userId;
        String nickname;
        double totalWater;
        String profileImageUrl; // Thêm trường profileImageUrl

        RankingEntry(String userId, String nickname, double totalWater, String profileImageUrl) {
            this.userId = userId;
            this.nickname = nickname;
            this.totalWater = totalWater;
            this.profileImageUrl = profileImageUrl;
        }

        @Override
        public String toString() {
            return "RankingEntry{userId='" + userId + "', nickname='" + nickname + "', totalWater=" + totalWater + ", profileImageUrl='" + profileImageUrl + "'}";
        }
    }

    public void bindRankingEntries(View top1View, View top2View, View top3View, List<RankingEntry> entries) {
        // Reset visibility
        if (top1View != null) top1View.setVisibility(View.GONE);
        if (top2View != null) top2View.setVisibility(View.GONE);
        if (top3View != null) top3View.setVisibility(View.GONE);

        // Bind data
        if (entries.size() >= 1) {
            bindEntry(top1View, entries.get(0), 1);
            if (top1View != null) top1View.setVisibility(View.VISIBLE);
        }
        if (entries.size() >= 2) {
            bindEntry(top2View, entries.get(1), 2);
            if (top2View != null) top2View.setVisibility(View.VISIBLE);
        }
        if (entries.size() >= 3) {
            bindEntry(top3View, entries.get(2), 3);
            if (top3View != null) top3View.setVisibility(View.VISIBLE);
        }
    }

    private void bindEntry(View view, RankingEntry entry, int rank) {
        if (view == null) {
            Log.e("RankingAdapter", "View for rank " + rank + " is null");
            return;
        }

        TextView rankText = view.findViewById(R.id.rank_text);
        TextView nameText = view.findViewById(R.id.name_text);
        TextView waterText = view.findViewById(R.id.water_text);
        ImageView userIcon = view.findViewById(R.id.user_icon);

        if (rankText == null) {
            Log.e("RankingAdapter", "rank_text not found for rank " + rank);
        } else {
            rankText.setText(String.valueOf(rank));
        }

        if (nameText == null) {
            Log.e("RankingAdapter", "name_text not found for rank " + rank);
        } else {
            nameText.setText(entry.nickname);
        }

        if (waterText == null) {
            Log.e("RankingAdapter", "water_text not found for rank " + rank);
        } else {
            waterText.setText(String.format("%.0f ml", entry.totalWater));
        }

        if (userIcon == null) {
            Log.e("RankingAdapter", "user_icon not found for rank " + rank);
        } else {
            // Nếu có profileImageUrl, tải ảnh bằng Glide; nếu không, dùng icon mặc định
            if (entry.profileImageUrl != null && !entry.profileImageUrl.isEmpty()) {
                Glide.with(view.getContext())
                        .load(entry.profileImageUrl)
                        .placeholder(R.drawable.ic_user_placeholder)
                        .error(R.drawable.ic_user_placeholder)
                        .into(userIcon);
            } else {
                userIcon.setImageResource(R.drawable.ic_user_placeholder);
            }
        }
    }
}