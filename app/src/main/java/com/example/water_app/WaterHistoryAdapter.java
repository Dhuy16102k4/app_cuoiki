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
                .inflate(R.layout.item_water_history, parent, false);
        return new WaterHistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull WaterHistoryViewHolder holder, int position) {
        WaterEntry entry = waterEntries.get(position);
        holder.drinkType.setText(entry.drinkType);
        holder.amount.setText(String.format("%.0f ml", entry.amount));
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        holder.time.setText(sdf.format(new Date(entry.timestamp)));
    }

    @Override
    public int getItemCount() {
        return waterEntries.size();
    }

    static class WaterHistoryViewHolder extends RecyclerView.ViewHolder {
        TextView drinkType, amount, time;

        WaterHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            drinkType = itemView.findViewById(R.id.drink_type);
            amount = itemView.findViewById(R.id.amount);
            time = itemView.findViewById(R.id.time);
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