package com.example.water_app;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import android.view.WindowManager;
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

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
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
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends BaseActivity {

    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private Calendar currentWeekStart;
    private double waterGoal = 2000; // Default, updated from Firestore/SharedPreferences
    private final List<DayData> weekDays = new ArrayList<>();
    private static final int STORAGE_PERMISSION_CODE = 101;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle(R.string.statistics_title);
        setSupportActionBar(toolbar);

        // Set white overflow icon
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setColorFilter(ContextCompat.getColor(this, android.R.color.white), PorterDuff.Mode.SRC_IN);
            toolbar.setOverflowIcon(overflowIcon);
        }

        // Setup UI components (local variables)
        TextView weekLabel = findViewById(R.id.week_label);
        Button prevWeekButton = findViewById(R.id.prev_week_button);
        Button nextWeekButton = findViewById(R.id.next_week_button);
        RecyclerView daysRecyclerView = findViewById(R.id.days_recycler_view);
        DayAdapter dayAdapter = new DayAdapter();
        daysRecyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
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

        // Update week label
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        String startDate = sdf.format(currentWeekStart.getTime());
        String endDate = sdf.format(weekEnd.getTime());
        weekLabel.setText(getString(R.string.week_label, startDate, endDate));

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
        } else if (item.getItemId() == R.id.action_ranking) {
            fetchRankingData();
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
                    .setTitle(R.string.storage_permission_request_title)
                    .setMessage(R.string.storage_permission_request_message)
                    .setPositiveButton(R.string.settings, (d, w) -> {
                        requestPermissions(new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, STORAGE_PERMISSION_CODE);
                    })
                    .setNegativeButton(R.string.cancel, (d, w) -> {
                        Toast.makeText(this, R.string.export_canceled, Toast.LENGTH_SHORT).show();
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
                Toast.makeText(this, R.string.permission_granted, Toast.LENGTH_SHORT).show();
                exportToPdf();
            } else {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.permission_denied_title)
                        .setMessage(R.string.permission_denied_message)
                        .setPositiveButton(R.string.settings, (d, w) -> {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton(R.string.cancel, null)
                        .show();
            }
        }
    }

    private void exportToPdf() {
        try {
            FirebaseUser currentUser = auth.getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(this, R.string.login_required_export, Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = currentUser.getUid();
            SimpleDateFormat sdf = new SimpleDateFormat("ddMMyyyy_HHmmss", Locale.getDefault());
            String fileName = "WaterIntake_" + sdf.format(System.currentTimeMillis()) + ".pdf";
            File file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), fileName);

            PdfWriter writer = new PdfWriter(file);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            document.add(new Paragraph("Lịch sử uống nước")
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
                    table.addCell(new Cell().add(new Paragraph(String.format(Locale.getDefault(), "%.0f", entry.amount))));
                    SimpleDateFormat timeFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
                    table.addCell(new Cell().add(new Paragraph(timeFormat.format(entry.timestamp))));
                }
            }

            document.add(table);
            document.close();

            Toast.makeText(this, getString(R.string.pdf_exported, file.getAbsolutePath()), Toast.LENGTH_LONG).show();
            Log.d("StatisticsActivity", "PDF exported to: " + file.getAbsolutePath());
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.pdf_export_error, e.getMessage()), Toast.LENGTH_SHORT).show();
            Log.e("StatisticsActivity", "PDF export error: " + e.getMessage());
        }
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.isConnectedOrConnecting();
    }

    @SuppressWarnings({"ConstantConditions", "NotifyDataSetChanged"})
    private void fetchRankingData() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            return;
        }

        // Use last week instead of current week for testing
        Calendar lastWeekStart = (Calendar) currentWeekStart.clone();
        lastWeekStart.add(Calendar.WEEK_OF_YEAR, -1); // Go back one week

        long startTimestamp = lastWeekStart.getTimeInMillis();
        Calendar lastWeekEnd = (Calendar) lastWeekStart.clone();
        lastWeekEnd.add(Calendar.DAY_OF_YEAR, 6);
        lastWeekEnd.set(Calendar.HOUR_OF_DAY, 23);
        lastWeekEnd.set(Calendar.MINUTE, 59);
        lastWeekEnd.set(Calendar.SECOND, 59);
        lastWeekEnd.set(Calendar.MILLISECOND, 999);
        long endTimestamp = lastWeekEnd.getTimeInMillis();

        // Log the date range for debugging
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Log.d("StatisticsActivity", "Fetching ranking for last week: " + sdf.format(lastWeekStart.getTime()) + " to " + sdf.format(lastWeekEnd.getTime()));

        // Fetch all users
        db.collection("users").get()
                .addOnSuccessListener(userSnapshots -> {
                    Map<String, String> userNicknames = new HashMap<>();
                    Map<String, String> userProfileImages = new HashMap<>(); // Lưu profileImageUrl
                    Map<String, Double> userWaterGoals = new HashMap<>();
                    Map<String, Map<Long, Double>> userDailyTotals = new HashMap<>();
                    Map<String, Double> userWeeklyTotals = new HashMap<>();

                    // Initialize user data
                    for (var userDoc : userSnapshots) {
                        String userId = userDoc.getId();
                        String nickname = userDoc.getString("nickname");
                        String email = userDoc.getString("email");
                        String profileImageUrl = userDoc.getString("profileImageUrl"); // Lấy profileImageUrl
                        Double waterGoal = userDoc.getDouble("waterGoal");
                        if (nickname == null && email != null) {
                            nickname = email.split("@")[0]; // Fallback: Use email prefix as nickname
                        }
                        if (nickname != null) {
                            userNicknames.put(userId, nickname);
                            userProfileImages.put(userId, profileImageUrl); // Lưu profileImageUrl (có thể null)
                            userWaterGoals.put(userId, waterGoal != null ? waterGoal : 2000.0);
                            userDailyTotals.put(userId, new HashMap<>());
                            userWeeklyTotals.put(userId, 0.0);
                        } else {
                            Log.w("StatisticsActivity", "Skipping user " + userId + ": No nickname or email");
                        }
                    }

                    // Fetch water history for all users in the week
                    db.collectionGroup("waterHistory")
                            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                            .whereLessThanOrEqualTo("timestamp", endTimestamp)
                            .get()
                            .addOnSuccessListener(historySnapshots -> {
                                // Aggregate daily totals
                                for (var doc : historySnapshots) {
                                    DocumentReference userDocRef = doc.getReference().getParent().getParent();
                                    if (userDocRef == null) {
                                        Log.w("StatisticsActivity", "Parent document is null for waterHistory entry: " + doc.getId());
                                        continue;
                                    }
                                    String userId = userDocRef.getId();
                                    Double amount = doc.getDouble("amount");
                                    Long timestamp = doc.getLong("timestamp");
                                    if (amount != null && timestamp != null && userDailyTotals.containsKey(userId)) {
                                        long dayStart = getDayStartTimestamp(timestamp);

                                        Map<Long, Double> dailyTotals = userDailyTotals.get(userId);
                                        dailyTotals.put(dayStart, dailyTotals.getOrDefault(dayStart, 0.0) + amount);

                                        userWeeklyTotals.put(userId, userWeeklyTotals.get(userId) + amount);
                                    }
                                }
//test
                                // double weeklyTotal = userWeeklyTotals.getOrDefault(userId, 0.0);
                                //                                    if (weeklyTotal > 0) { // Chỉ thêm người dùng có dữ liệu
                                //                                        rankingEntries.add(new RankingAdapter.RankingEntry(
                                //            userId, userNicknames.get(userId), userWeeklyTotals.get(userId), userProfileImages.get(userId)));
                                // Identify users who meet waterGoal every day
                                List<RankingAdapter.RankingEntry> rankingEntries = new ArrayList<>();
                                for (String userId : userNicknames.keySet()) {
                                    double weeklyTotal = userWeeklyTotals.getOrDefault(userId, 0.0);
                                    Map<Long, Double> dailyTotals = userDailyTotals.get(userId);
                                    if (weeklyTotal > 0) { // Chỉ thêm người dùng có dữ liệu
                                        rankingEntries.add(new RankingAdapter.RankingEntry(
                                                userId, userNicknames.get(userId), userWeeklyTotals.get(userId), userProfileImages.get(userId)));

                                    }
                                    // double waterGoal = userWaterGoals.get(userId);
                                    //boolean meetsDailyGoal = true;

                                    // Check each day of the week
//                                    Calendar dayCal = (Calendar) lastWeekStart.clone();
//                                    for (int i = 0; i < 7; i++) {
//                                        long dayStart = dayCal.getTimeInMillis();
//                                        double dailyTotal = dailyTotals.getOrDefault(dayStart, 0.0);
//                                        if (dailyTotal < waterGoal) {
//                                            meetsDailyGoal = false;
//                                            break;
//                                        }
//                                        dayCal.add(Calendar.DAY_OF_YEAR, 1);
//                                    }

                                    // Add to ranking if all days meet goal
//                                    if (meetsDailyGoal) {
//                                        rankingEntries.add(new RankingAdapter.RankingEntry(
//                                                userId, userNicknames.get(userId), userWeeklyTotals.get(userId), userProfileImages.get(userId)));
//                                    }
                                }

                                // Sort by weekly total (descending) and limit to top 3
                                rankingEntries.sort((a, b) -> Double.compare(b.getTotalWater(), a.getTotalWater()));
                                if (rankingEntries.size() > 3) {
                                    rankingEntries = rankingEntries.subList(0, 3);
                                }

                                // Show ranking dialog
                                showRankingDialog(rankingEntries);
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, getString(R.string.ranking_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                                Log.e("StatisticsActivity", "Error fetching ranking history: " + e.getMessage());
                            });
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.users_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                    Log.e("StatisticsActivity", "Error fetching users: " + e.getMessage());
                });
    }


    private long getDayStartTimestamp(long timestamp) {
        Calendar entryCal = Calendar.getInstance();
        entryCal.setTimeInMillis(timestamp);
        entryCal.set(Calendar.HOUR_OF_DAY, 0);
        entryCal.set(Calendar.MINUTE, 0);
        entryCal.set(Calendar.SECOND, 0);
        entryCal.set(Calendar.MILLISECOND, 0);
        return entryCal.getTimeInMillis();
    }

    private void showRankingDialog(List<RankingAdapter.RankingEntry> rankingEntries) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_ranking, null);
        builder.setView(dialogView);

        // Tìm các thành phần trong dialog
        TextView emptyText = dialogView.findViewById(R.id.empty_text);
        View top1View = dialogView.findViewById(R.id.top1_item);
        View top2View = dialogView.findViewById(R.id.top2_item);
        View top3View = dialogView.findViewById(R.id.top3_item);
        Button closeButton = dialogView.findViewById(R.id.close_button);

        // Kiểm tra null để tránh crash
        if (emptyText == null || top1View == null || top2View == null || top3View == null || closeButton == null) {
            Log.e("StatisticsActivity", "One or more views in dialog_ranking.xml are null");
            Toast.makeText(this, "Lỗi hiển thị bảng xếp hạng", Toast.LENGTH_SHORT).show();
            return;
        }

        RankingAdapter rankingAdapter = new RankingAdapter();

        if (rankingEntries == null || rankingEntries.isEmpty()) {
            emptyText.setVisibility(View.VISIBLE);
            top1View.setVisibility(View.GONE);
            top2View.setVisibility(View.GONE);
            top3View.setVisibility(View.GONE);
        } else {
            emptyText.setVisibility(View.GONE);
            rankingAdapter.bindRankingEntries(top1View, top2View, top3View, rankingEntries);
        }

        Log.d("StatisticsActivity", "Showing ranking dialog with " + (rankingEntries != null ? rankingEntries.size() : 0) + " entries");

        AlertDialog dialog = builder.create();

        // Tùy chỉnh kích thước dialog
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Background trong suốt để thấy background của dialog
        WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
        params.width = WindowManager.LayoutParams.WRAP_CONTENT; // Chiều rộng dựa trên nội dung
        params.height = WindowManager.LayoutParams.WRAP_CONTENT; // Chiều cao dựa trên nội dung
        dialog.getWindow().setAttributes(params);

        closeButton.setOnClickListener(v -> dialog.dismiss());
        dialog.show();
    }

    private void fetchWaterGoal() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.login_required_statistics, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();
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

    @SuppressWarnings("NotifyDataSetChanged")
    private void fetchWeekData() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(this, R.string.login_required_statistics, Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        String userId = currentUser.getUid();
        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);
        weekEnd.set(Calendar.MILLISECOND, 999);

        long startTimestamp = currentWeekStart.getTimeInMillis();
        long endTimestamp = weekEnd.getTimeInMillis();

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
                        if (drinkType != null && timestamp != null) {
                            weekEntries.add(new WaterHistoryAdapter.WaterEntry(drinkType, amount != null ? amount : 0.0, timestamp));
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

                    RecyclerView daysRecyclerView = findViewById(R.id.days_recycler_view);
                    DayAdapter dayAdapter = (DayAdapter) daysRecyclerView.getAdapter();
                    if (dayAdapter != null) {
                        // notifyDataSetChanged is used because weekDays is fully replaced
                        dayAdapter.notifyDataSetChanged();
                    }
                    Log.d("StatisticsActivity", "Updated weekDays with " + weekDays.size() + " days");
                    checkWeekCompletion();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, getString(R.string.data_load_error, e.getMessage()), Toast.LENGTH_SHORT).show();
                    weekDays.clear();
                    RecyclerView daysRecyclerView = findViewById(R.id.days_recycler_view);
                    DayAdapter dayAdapter = (DayAdapter) daysRecyclerView.getAdapter();
                    if (dayAdapter != null) {
                        // notifyDataSetChanged is used because weekDays is fully replaced
                        dayAdapter.notifyDataSetChanged();
                    }
                    Log.e("StatisticsActivity", "Error fetching week data: " + e.getMessage());
                });
    }

    private void checkWeekCompletion() {
        // Kiểm tra xem tuần đã hoàn thành chưa (7 ngày đã qua)
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);

        Calendar weekEnd = (Calendar) currentWeekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6); // Ngày cuối của tuần
        weekEnd.set(Calendar.HOUR_OF_DAY, 23);
        weekEnd.set(Calendar.MINUTE, 59);
        weekEnd.set(Calendar.SECOND, 59);
        weekEnd.set(Calendar.MILLISECOND, 999);

        // Nếu tuần chưa kết thúc (hôm nay chưa qua ngày cuối của tuần), không hiển thị thông báo
        if (today.before(weekEnd)) {
            Log.d("StatisticsActivity", "Week not yet completed, skipping congratulation");
            return;
        }

        // Kiểm tra xem cả 7 ngày có đạt mục tiêu không
        boolean allDaysMetGoal = true;
        if (weekDays.size() != 7) {
            // Nếu không có đủ 7 ngày trong dữ liệu, không hiển thị thông báo
            Log.d("StatisticsActivity", "Not enough days in weekDays: " + weekDays.size());
            return;
        }

        for (DayData day : weekDays) {
            if (day.totalWater < waterGoal) {
                allDaysMetGoal = false;
                break;
            }
        }

        // Hiển thị dialog chúc mừng nếu cả 7 ngày đều đạt mục tiêu
        if (allDaysMetGoal) {
            showCongratulationDialog();
            Log.d("StatisticsActivity", "All 7 days met goal, showing congratulation dialog");
        } else {
            Log.d("StatisticsActivity", "Not all days met goal, skipping congratulation");
        }
    }
    private void showCongratulationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_congratulations, null);
        builder.setView(dialogView);

        // Tìm các thành phần trong dialog
        ImageView congratsGif = dialogView.findViewById(R.id.congrats_gif);
        TextView congratsMessage = dialogView.findViewById(R.id.congrats_message);
        Button closeButton = dialogView.findViewById(R.id.close_button);

        // Tải GIF bằng Glide
        Glide.with(this)
                .asGif()
                .load(R.raw.congratulations) // Thay bằng tên file GIF của bạn
                .placeholder(R.drawable.check_mark) // Hình ảnh placeholder nếu GIF chưa tải
                .error(R.drawable.ic_red_x) // Hình ảnh nếu không tải được GIF
                .into(congratsGif);

        // Đặt thông điệp chúc mừng
        congratsMessage.setText(R.string.week_completion_message);

        // Hiển thị dialog
        AlertDialog dialog = builder.create();
        dialog.setCanceledOnTouchOutside(false); // Không cho phép đóng dialog khi nhấn ngoài
        dialog.show();

        // Xử lý nút đóng
        closeButton.setOnClickListener(v -> dialog.dismiss());
    }
    private static class DayData {
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
            holder.waterStatus.setText(getString(R.string.water_amount, day.totalWater));

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
                        day.totalWater >= waterGoal ? R.drawable.check_mark : R.drawable.ic_red_x
                );
            } else if (dayCal.equals(today)) {
                // Current day
                holder.statusIcon.setImageResource(
                        day.totalWater >= waterGoal ? R.drawable.check_mark : R.drawable.ic_loading
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
        dialogTitle.setText(getString(R.string.day_history_title, sdf.format(day.date)));

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