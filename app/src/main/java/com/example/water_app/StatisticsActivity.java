package com.example.water_app;

import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.properties.TextAlignment;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class StatisticsActivity extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private RecyclerView daysRecyclerView;
    private DayAdapter dayAdapter;
    private TextView weekLabel;
    private Button prevWeekButton;
    private Button nextWeekButton;
    private Calendar currentWeekStart;
    private double waterGoal = 2000; // Default, updated from Firestore/SharedPreferences
    private List<DayData> weekDays = new ArrayList<>();
    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Thống kê");
        setSupportActionBar(toolbar);

        // Setup UI components
        weekLabel = findViewById(R.id.week_label);
        prevWeekButton = findViewById(R.id.prev_week_button);
        nextWeekButton = findViewById(R.id.next_week_button);
        daysRecyclerView = findViewById(R.id.days_recycler_view);
        daysRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        dayAdapter = new DayAdapter();
        daysRecyclerView.setAdapter(dayAdapter);

        // Initialize week
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);

        // Setup bottom navigation
        setupBottomNavigation();

        // Setup week navigation
        prevWeekButton.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
            nextWeekButton.setEnabled(true);
            fetchWeekData();
        });

        nextWeekButton.setOnClickListener(v -> {
            currentWeekStart.add(Calendar.WEEK_OF_YEAR, 1);
            Calendar now = Calendar.getInstance();
            if (currentWeekStart.after(now)) {
                currentWeekStart.add(Calendar.WEEK_OF_YEAR, -1);
                nextWeekButton.setEnabled(false);
            }
            fetchWeekData();
        });

        // Fetch initial data
        fetchWaterGoal();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_statistics, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_export) {
            showExportFormatDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showExportFormatDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_export_format, null);
        builder.setView(dialogView);

        Button exportPdfButton = dialogView.findViewById(R.id.export_pdf_button);
        Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        AlertDialog dialog = builder.create();

        exportPdfButton.setOnClickListener(v -> {
            dialog.dismiss();
            checkStoragePermission();
        });

        cancelButton.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    private void checkStoragePermission() {
        // Skip permission for Android 13+ when writing to Downloads
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            exportToPdf();
            return;
        }

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Explain why permission is needed
            new AlertDialog.Builder(this)
                    .setTitle("Yêu cầu quyền lưu trữ")
                    .setMessage("Ứng dụng cần quyền lưu trữ để xuất dữ liệu uống nước ra file PDF. Vui lòng cấp quyền để tiếp tục.")
                    .setPositiveButton("Cấp quyền", (d, w) -> {
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                    })
                    .setNegativeButton("Hủy", (d, w) -> {
                        Toast.makeText(this, "Xuất dữ liệu bị hủy do thiếu quyền lưu trữ", Toast.LENGTH_SHORT).show();
                    })
                    .show();
        } else {
            exportToPdf();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền lưu trữ được cấp", Toast.LENGTH_SHORT).show();
                exportToPdf();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle("Quyền lưu trữ bị từ chối")
                        .setMessage("Không thể xuất dữ liệu do thiếu quyền lưu trữ. Bạn có muốn vào Cài đặt để bật quyền này không?")
                        .setPositiveButton("Cài đặt", (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Hủy", null)
                        .show();
            }
        }
    }

    private void exportToPdf() {
        try {
            String userId = auth.getCurrentUser().getUid();
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault());
            String fileName = "WaterIntake_" + sdf.format(System.currentTimeMillis()) + ".pdf";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Lich su uong nuoc")
                    .setFontSize(18)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER));

            float[] columnWidths = {200, 100, 100};
            Table table = new Table(columnWidths);
            table.addHeaderCell(new Cell().add(new Paragraph("Loại nước").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Lượng (ml)").setBold()));
            table.addHeaderCell(new Cell().add(new Paragraph("Thời gian").setBold()));

            for (DayData day : weekDays) {
                for (WaterHistoryAdapter.WaterEntry entry : day.entries) {
                    table.addCell(new Cell().add(new Paragraph(entry.drinkType)));
                    table.addCell(new Cell().add(new Paragraph(String.format("%.0f", entry.amount))));
                    SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    table.addCell(new Cell().add(new Paragraph(timeFormat.format(entry.timestamp))));
                }
            }

            document.add(table);
            document.close();

            Toast.makeText(this, "Đã xuất PDF: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            Log.d("StatisticsActivity", "PDF exported to: " + file.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, "Lỗi khi xuất PDF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            Log.e("StatisticsActivity", "PDF export error: " + e.getMessage());
        }
    }

    private void fetchWaterGoal() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    Double goal = document.getDouble("waterGoal");
                    waterGoal = goal != null ? goal : 2000;
                    Log.d("StatisticsActivity", "Water goal fetched: " + waterGoal);
                    fetchWeekData();
                })
                .addOnFailureListener(e -> {
                    waterGoal = 2000;
                    Log.e("StatisticsActivity", "Error fetching water goal: " + e.getMessage());
                    fetchWeekData();
                });
    }

    private void fetchWeekData() {
        String userId = auth.getCurrentUser().getUid();
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);
        weekEnd.set(Calendar.MILLISECOND, 999);

        long startTimestamp = currentWeekStart.getTimeInMillis();
        long endTimestamp = weekEnd.getTimeInMillis();

        // Update week label
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        weekLabel.setText(String.format("Tuần: %s - %s", sdf.format(currentWeekStart.getTime()), sdf.format(weekEnd.getTime())));

        // Fetch water history for the week
        db.collection("users").document(userId).collection("waterHistory")
                .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                .whereLessThanOrEqualTo("timestamp", endTimestamp)
                .orderBy("timestamp", Query.Direction.ASCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    weekDays.clear();
                    List<WaterHistoryAdapter.WaterEntry> weekEntries = new ArrayList<>();
                    Log.d("StatisticsActivity", "Fetched " + querySnapshot.size() + " water entries");

                    for (var doc : querySnapshot) {
                        String drinkType = doc.getString("drinkType");
                        Double amount = doc.getDouble("amount");
                        Long timestamp = doc.getLong("timestamp");
                        if (drinkType != null && amount != null && timestamp != null) {
                            weekEntries.add(new WaterHistoryAdapter.WaterEntry(drinkType, amount, timestamp));
                        }
                    }

                    // Group entries by day
                    Calendar dayStart = (Calendar) currentWeekStart.clone();
                    for (int i = 0; i < 7; i++) {
                        long dayStartTime = dayStart.getTimeInMillis();
                        long dayEndTime = dayStartTime + 24 * 60 * 60 * 1000 - 1;
                        double dailyTotal = 0;
                        List<WaterHistoryAdapter.WaterEntry> dailyEntries = new ArrayList<>();

                        for (WaterHistoryAdapter.WaterEntry entry : weekEntries) {
                            if (entry.timestamp >= dayStartTime && entry.timestamp <= dayEndTime) {
                                dailyTotal += entry.amount;
                                dailyEntries.add(entry);
                            }
                        }

                        weekDays.add(new DayData(dayStart.getTimeInMillis(), dailyTotal, dailyEntries));
                        dayStart.add(Calendar.DAY_OF_YEAR, 1);
                    }

                    dayAdapter.notifyDataSetChanged();
                    Log.d("StatisticsActivity", "Updated weekDays with " + weekDays.size() + " days");
                    checkWeekCompletion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    weekDays.clear();
                    dayAdapter.notifyDataSetChanged();
                    Log.e("StatisticsActivity", "Error fetching week data: " + e.getMessage());
                });
    }

    private void checkWeekCompletion() {
        boolean allDaysMetGoal = true;
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        for (DayData day : weekDays) {
            Calendar dayCal = Calendar.getInstance();
            dayCal.setTimeInMillis(day.date);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);

            // Only check past and current days for completion
            if (!dayCal.after(today)) {
                if (day.totalWater < waterGoal) {
                    allDaysMetGoal = false;
                    break;
                }
            }
        }
        if (allDaysMetGoal && !weekDays.isEmpty()) {
            Toast.makeText(this, "Chúc mừng! Bạn đã hoàn thành mục tiêu uống nước cả tuần!", Toast.LENGTH_LONG).show();
            Log.d("StatisticsActivity", "All days met goal, showing toast");
        }
    }

    private class DayData {
        long date;
        double totalWater;
        List<WaterHistoryAdapter.WaterEntry> entries;

        DayData(long date, double totalWater, List<WaterHistoryAdapter.WaterEntry> entries) {
            this.date = date;
            this.totalWater = totalWater;
            this.entries = entries;
        }
    }

    private class DayAdapter extends RecyclerView.Adapter<DayAdapter.ViewHolder> {

        class ViewHolder extends RecyclerView.ViewHolder {
            TextView dayName;
            TextView dayDate;
            TextView waterStatus;
            ImageView statusIcon;
            androidx.cardview.widget.CardView cardView;

            ViewHolder(View itemView) {
                super(itemView);
                dayName = itemView.findViewById(R.id.day_name);
                dayDate = itemView.findViewById(R.id.day_date);
                waterStatus = itemView.findViewById(R.id.water_status);
                statusIcon = itemView.findViewById(R.id.status_icon);
                cardView = (androidx.cardview.widget.CardView) itemView;
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            DayData day = weekDays.get(position);
            SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            holder.dayDate.setText(dateFormat.format(day.date));
            holder.waterStatus.setText(String.format("%.0f ml", day.totalWater));

            // Set day name (Thứ Hai to Chủ Nhật)
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(day.date);
            String[] dayNames = {"Chủ Nhật", "Thứ Hai", "Thứ Ba", "Thứ Tư", "Thứ Năm", "Thứ Sáu", "Thứ Bảy"};
            int dayOfWeek = calendar.get(Calendar.DAY_OF_WEEK) - 1; // Adjust for 0-based index
            holder.dayName.setText(dayNames[dayOfWeek]);

            // Determine day status (past, current, future)
            Calendar today = Calendar.getInstance();
            today.set(Calendar.HOUR_OF_DAY, 0);
            today.set(Calendar.MINUTE, 0);
            today.set(Calendar.SECOND, 0);
            today.set(Calendar.MILLISECOND, 0);

            Calendar dayCal = Calendar.getInstance();
            dayCal.setTimeInMillis(day.date);
            dayCal.set(Calendar.HOUR_OF_DAY, 0);
            dayCal.set(Calendar.MINUTE, 0);
            dayCal.set(Calendar.SECOND, 0);
            dayCal.set(Calendar.MILLISECOND, 0);

            if (dayCal.before(today)) {
                // Past day
                holder.statusIcon.setImageResource(
                        day.totalWater >= waterGoal ? R.drawable.ic_checkmark : R.drawable.ic_red_x
                );
            } else if (dayCal.equals(today)) {
                // Current day
                holder.statusIcon.setImageResource(
                        day.totalWater >= waterGoal ? R.drawable.ic_checkmark : R.drawable.ic_loading
                );
            } else {
                // Future day
                holder.statusIcon.setImageResource(R.drawable.ic_loading);
            }

            Log.d("DayAdapter", "Binding day: " + dayNames[dayOfWeek] + ", Date: " + dateFormat.format(day.date) +
                    ", Total: " + day.totalWater + ", Icon: " + (dayCal.before(today) ?
                    (day.totalWater >= waterGoal ? "Checkmark" : "Red X") :
                    (dayCal.equals(today) ? (day.totalWater >= waterGoal ? "Checkmark" : "Loading") : "Loading")));

            // Show dialog on click
            holder.cardView.setOnClickListener(v -> showDayHistoryDialog(day));
        }

        @Override
        public int getItemCount() {
            return weekDays.size();
        }
    }

    private void showDayHistoryDialog(DayData day) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_day_history, null);
        builder.setView(dialogView);

        TextView dialogTitle = dialogView.findViewById(R.id.dialog_title);
        RecyclerView historyRecyclerView = dialogView.findViewById(R.id.history_recycler_view);
        Button closeButton = dialogView.findViewById(R.id.close_button);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        dialogTitle.setText("Lịch sử uống nước - " + sdf.format(day.date));

        historyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        WaterHistoryAdapter historyAdapter = new WaterHistoryAdapter();
        historyRecyclerView.setAdapter(historyAdapter);
        historyAdapter.setWaterEntries(day.entries);
        Log.d("StatisticsActivity", "Showing dialog with " + day.entries.size() + " entries for " + sdf.format(day.date));

        AlertDialog dialog = builder.create();
        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_statistics;
    }
}