package com.example.water_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.*;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.example.water_app.auth.EditProfileActivity;
import com.example.water_app.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressBar waterProgressBar;
    private TextView waterStatusText;
    private Button addWaterButton;
    private RecyclerView waterHistoryRecyclerView;
    private WaterHistoryAdapter waterHistoryAdapter;
    private TextView currentDateText;
    private double waterGoal = 0;
    private double drankWater = 0;
    private boolean hasShownToast = false;
    private List<WaterHistoryAdapter.WaterEntry> waterEntries = new ArrayList<>();
    private String selectedSoundName = "sound1";
    private int selectedSoundResId = R.raw.sound1;
    private int[] soundResIds = { R.raw.sound1, R.raw.sound2, R.raw.sound3 };
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

        // Initialize sound
        int selectedSoundIndex = prefs.getInt("selected_sound", 0);
        if (selectedSoundIndex < 0 || selectedSoundIndex >= soundResIds.length) {
            selectedSoundIndex = 0;
        }
        selectedSoundResId = soundResIds[selectedSoundIndex];

        // Check if user is logged in
        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Setup toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Water Reminder");
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.menu_profile) {
                startActivity(new Intent(this, EditProfileActivity.class));
                return true;
            } else if (id == R.id.menu_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            } else if (id == R.id.menu_logout) {
                auth.signOut();
                // Reset state
                waterGoal = 0;
                drankWater = 0;
                waterEntries.clear();
                hasShownToast = false;
                prefs.edit().clear().apply();
                waterHistoryAdapter.setWaterEntries(waterEntries);
                updateWaterStatus();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });
        Drawable overflowIcon = toolbar.getOverflowIcon();
        if (overflowIcon != null) {
            overflowIcon.setTint(getResources().getColor(android.R.color.white, null));
            toolbar.setOverflowIcon(overflowIcon);
        }

        // Initialize UI components
        waterProgressBar = findViewById(R.id.waterProgressBar);
        waterStatusText = findViewById(R.id.waterStatusText);
        addWaterButton = findViewById(R.id.addWaterButton);
        waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);
        currentDateText = findViewById(R.id.currentDateText);

        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        waterHistoryAdapter = new WaterHistoryAdapter();
        waterHistoryRecyclerView.setAdapter(waterHistoryAdapter);

        // Set current date
        updateCurrentDate();

        setupBottomNavigation();

        // Reset hasShownToast daily
        resetDailyToastFlag();

        // Fetch data and update UI
        fetchWaterData();
        addWaterButton.setOnClickListener(v -> showAddWaterDialog());
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (auth.getCurrentUser() != null) {
            fetchWaterData();
            updateCurrentDate();
        }
    }

    private void updateCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String currentDate = sdf.format(new Date());
        currentDateText.setText(currentDate);
    }

    private void resetDailyToastFlag() {
        long lastReset = prefs.getLong("last_toast_reset", 0);
        Calendar now = Calendar.getInstance();
        Calendar lastResetCal = Calendar.getInstance();
        lastResetCal.setTimeInMillis(lastReset);

        if (now.get(Calendar.DAY_OF_YEAR) != lastResetCal.get(Calendar.DAY_OF_YEAR) ||
                now.get(Calendar.YEAR) != lastResetCal.get(Calendar.YEAR)) {
            hasShownToast = false;
            prefs.edit().putBoolean("has_shown_toast", false)
                    .putLong("last_toast_reset", now.getTimeInMillis())
                    .apply();
        } else {
            hasShownToast = prefs.getBoolean("has_shown_toast", false);
        }
    }

    private void fetchWaterData() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double goal = document.getDouble("waterGoal");
                        waterGoal = goal != null ? goal : 2000;
                        Log.d("MainActivity", "Water goal fetched: " + waterGoal);
                    } else {
                        waterGoal = 2000;
                        Log.d("MainActivity", "No document found, using default water goal: 2000");
                    }
                    fetchWaterHistory();
                })
                .addOnFailureListener(e -> {
                    waterGoal = 2000;
                    Log.e("MainActivity", "Error fetching water goal: " + e.getMessage());
                    fetchWaterHistory();
                });
    }

    private void importTestWaterData() {
        String userId = auth.getCurrentUser().getUid();
        prefs.edit().putBoolean("test_data_imported", false).apply();
        boolean isTestDataImported = prefs.getBoolean("test_data_imported", false);
        if (isTestDataImported) {
            return;
        }

        Calendar calendar = Calendar.getInstance();
        calendar.set(2025, Calendar.APRIL, 20, 0, 0, 0);
        Calendar endDate = Calendar.getInstance();
        endDate.set(2025, Calendar.APRIL, 27, 23, 59, 59);

        String[] drinkTypes = {"Nước lọc", "Trà", "Nước lọc"};
        double[] amounts = {1000, 1000, 800};
        int[] hours = {8, 12, 18};

        while (calendar.getTimeInMillis() <= endDate.getTimeInMillis()) {
            for (int i = 0; i < 3; i++) {
                calendar.set(Calendar.HOUR_OF_DAY, hours[i]);
                calendar.set(Calendar.MINUTE, 0);
                calendar.set(Calendar.SECOND, 0);
                long timestamp = calendar.getTimeInMillis();
                double waterEquivalent = calculateWaterEquivalent(drinkTypes[i], amounts[i]);
                Map<String, Object> waterEntry = new HashMap<>();
                waterEntry.put("drinkType", drinkTypes[i]);
                waterEntry.put("amount", waterEquivalent);
                waterEntry.put("timestamp", timestamp);

                db.collection("users").document(userId).collection("waterHistory")
                        .document("test_" + timestamp)
                        .set(waterEntry)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("MainActivity", "Test data imported: " + timestamp);
                        })
                        .addOnFailureListener(e -> {
                            Log.e("MainActivity", "Error importing test data: " + e.getMessage());
                        });
            }
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        prefs.edit().putBoolean("test_data_imported", true).apply();
    }

    private void fetchWaterHistory() {
        String userId = auth.getCurrentUser().getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1;

        db.collection("users").document(userId).collection("waterHistory")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .addSnapshotListener((querySnapshot, error) -> {
                    if (error != null) {
                        Log.e("MainActivity", "Error fetching water history: " + error.getMessage());
                        waterEntries.clear();
                        drankWater = 0;
                        waterHistoryAdapter.setWaterEntries(waterEntries);
                        updateWaterStatus();
                        return;
                    }
                    waterEntries.clear();
                    drankWater = 0;
                    for (var doc : querySnapshot) {
                        String drinkType = doc.getString("drinkType");
                        Double amount = doc.getDouble("amount");
                        Long timestamp = doc.getLong("timestamp");
                        if (drinkType != null && amount != null && timestamp != null) {
                            waterEntries.add(new WaterHistoryAdapter.WaterEntry(drinkType, amount, timestamp));
                            drankWater += amount;
                        }
                    }
                    waterHistoryAdapter.setWaterEntries(waterEntries);
                    updateWaterStatus();
                    Log.d("MainActivity", "Water history updated: " + waterEntries.size() + " entries");
                });
    }

    private void showAddWaterDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_water, null);
        builder.setView(dialogView);

        CardView cardWater = dialogView.findViewById(R.id.cardWater);
        CardView cardTea = dialogView.findViewById(R.id.cardTea);
        CardView cardCoffee = dialogView.findViewById(R.id.cardCoffee);
        CardView cardMilk = dialogView.findViewById(R.id.cardMilk);
        Button amount200ml = dialogView.findViewById(R.id.amount200ml);
        Button amount500ml = dialogView.findViewById(R.id.amount500ml);
        Button amount1000ml = dialogView.findViewById(R.id.amount1000ml);
        EditText customAmountInput = dialogView.findViewById(R.id.customAmountInput);
        Button confirmButton = dialogView.findViewById(R.id.confirmButton);
        ImageButton closeButton = dialogView.findViewById(R.id.closeButton);

        final String[] selectedDrinkType = {"Nước lọc"};
        final double[] selectedAmount = {0};

        View.OnClickListener cardClickListener = v -> {
            cardWater.setCardBackgroundColor(getResources().getColor(R.color.card_background, null));
            cardTea.setCardBackgroundColor(getResources().getColor(R.color.card_background, null));
            cardCoffee.setCardBackgroundColor(getResources().getColor(R.color.card_background, null));
            cardMilk.setCardBackgroundColor(getResources().getColor(R.color.card_background, null));
            ((CardView) v).setCardBackgroundColor(getResources().getColor(R.color.card_selected, null));

            if (v.getId() == R.id.cardWater) selectedDrinkType[0] = "Nước lọc";
            else if (v.getId() == R.id.cardTea) selectedDrinkType[0] = "Trà";
            else if (v.getId() == R.id.cardCoffee) selectedDrinkType[0] = "Cà phê";
            else if (v.getId() == R.id.cardMilk) selectedDrinkType[0] = "Sữa";
        };

        cardWater.setOnClickListener(cardClickListener);
        cardTea.setOnClickListener(cardClickListener);
        cardCoffee.setOnClickListener(cardClickListener);
        cardMilk.setOnClickListener(cardClickListener);

        amount200ml.setOnClickListener(v -> {
            selectedAmount[0] = 200;
            customAmountInput.setText("200");
        });
        amount500ml.setOnClickListener(v -> {
            selectedAmount[0] = 500;
            customAmountInput.setText("500");
        });
        amount1000ml.setOnClickListener(v -> {
            selectedAmount[0] = 1000;
            customAmountInput.setText("1000");
        });

        AlertDialog dialog = builder.create();

        closeButton.setOnClickListener(v -> dialog.dismiss());

        confirmButton.setOnClickListener(v -> {
            String customAmount = customAmountInput.getText().toString().trim();
            if (!customAmount.isEmpty()) {
                try {
                    selectedAmount[0] = Double.parseDouble(customAmount);
                } catch (NumberFormatException e) {
                    Toast.makeText(this, "Lượng nước không hợp lệ", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            if (selectedAmount[0] <= 0) {
                Toast.makeText(this, "Vui lòng chọn hoặc nhập lượng nước", Toast.LENGTH_SHORT).show();
                return;
            }

            double waterEquivalent = calculateWaterEquivalent(selectedDrinkType[0], selectedAmount[0]);
            saveWaterEntry(selectedDrinkType[0], waterEquivalent);
            dialog.dismiss();
        });

        cardWater.setCardBackgroundColor(getResources().getColor(R.color.card_selected, null));
        dialog.show();
    }

    private double calculateWaterEquivalent(String drinkType, double amount) {
        switch (drinkType) {
            case "Trà": return amount * 0.9;
            case "Cà phê": return amount * 0.8;
            case "Sữa": return amount * 0.85;
            default: return amount;
        }
    }

    private void saveWaterEntry(String drinkType, double amount) {
        String userId = auth.getCurrentUser().getUid();
        Map<String, Object> entry = new HashMap<>();
        entry.put("drinkType", drinkType);
        entry.put("amount", amount);
        entry.put("timestamp", System.currentTimeMillis());

        db.collection("users").document(userId).collection("waterHistory")
                .add(entry)
                .addOnSuccessListener(docRef -> {
                    Toast.makeText(this, "Đã thêm " + amount + "ml " + drinkType, Toast.LENGTH_SHORT).show();
                    SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                    boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);
                    if (isSoundEnabled) {
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, selectedSoundResId);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("MainActivity", "Error saving water entry: " + e.getMessage());
                });
    }

    private void updateWaterStatus() {
        if (waterGoal <= 0) {
            waterStatusText.setText("Đang tải dữ liệu...");
            waterProgressBar.setProgress(0);
            return;
        }

        waterStatusText.setText(String.format("Đã uống: %.0f ml / Còn thiếu: %.0f ml", drankWater, Math.max(0, waterGoal - drankWater)));
        int progress = (int) ((drankWater / waterGoal) * 100);
        waterProgressBar.setProgress(Math.min(progress, 100));

        if (drankWater >= waterGoal && !hasShownToast) {
            Toast.makeText(this, "Chúc mừng! Bạn đã uống đủ nước hôm nay!", Toast.LENGTH_LONG).show();
            hasShownToast = true;
            prefs.edit().putBoolean("has_shown_toast", true).apply();
        }

        if (drankWater > waterGoal && hasShownToast) {
            showCongratulationGif();
        }
    }

    private void showCongratulationGif() {
        FrameLayout congratsContainer = findViewById(R.id.congratsContainer);
        ImageView gifImageView = findViewById(R.id.gifImageView);
        TextView gifTextView = findViewById(R.id.gifTextView);

        congratsContainer.setVisibility(View.VISIBLE);
        Glide.with(this).load(R.drawable.congratulation_gif).into(gifImageView);
        gifTextView.setText("Tuyệt vời, bạn chăm lắm đó!");

        new Handler().postDelayed(() -> congratsContainer.setVisibility(View.GONE), 3000);
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }
}