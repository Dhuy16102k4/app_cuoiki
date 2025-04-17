package com.example.water_app;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WaterHistoryAdapter extends RecyclerView.Adapter<WaterHistoryAdapter.WaterHistoryViewHolder> {
    private List<WaterEntry> waterEntries = new ArrayList<>();

    public void setWaterEntries(List<WaterEntry> entries) {
        this.waterEntries = entries;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public WaterHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(android.R.layout.simple_list_item_2, parent, false);
        return new WaterHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaterHistoryViewHolder holder, int position) {
        WaterEntry entry = waterEntries.get(position);
        holder.text1.setText(String.format("%s: %.0f ml", entry.drinkType, entry.amount));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.text2.setText(sdf.format(new Date(entry.timestamp)));
    }

    @Override
    public int getItemCount() {
        return waterEntries.size();
    }

    static class WaterHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView text1, text2;

        WaterHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            text1 = itemView.findViewById(android.R.id.text1);
            text2 = itemView.findViewById(android.R.id.text2);
        }
    }

    static class WaterEntry {
        String drinkType;
        double amount;
        long timestamp;

        WaterEntry(String drinkType, double amount, long timestamp) {
            this.drinkType = drinkType;
            this.amount = amount;
            this.timestamp = timestamp;
        }
    }
}