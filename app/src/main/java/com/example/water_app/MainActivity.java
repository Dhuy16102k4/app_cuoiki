package com.example.water_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.view.View;
import android.widget.*;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water_app.auth.EditProfileActivity;
import com.example.water_app.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.*;

public class MainActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressBar waterProgressBar;
    private TextView waterStatusText;
    private Button addWaterButton;
    private RecyclerView waterHistoryRecyclerView;
    private WaterHistoryAdapter waterHistoryAdapter;
    private double waterGoal = 0;
    private double drankWater = 0;
    private List<WaterHistoryAdapter.WaterEntry> waterEntries = new ArrayList<>();
    private String selectedSoundName = "sound1";  // Mặc định là sound1
    private int selectedSoundResId = R.raw.sound1; // âm thanh mặc định
    private int[] soundResIds = { R.raw.sound1, R.raw.sound2, R.raw.sound3 }; // bạn có thể thêm các âm khác nếu có


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        SharedPreferences sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);
        int selectedSoundIndex = sharedPreferences.getInt("selected_sound", 0);
        if (selectedSoundIndex < 0 || selectedSoundIndex >= soundResIds.length) {
            selectedSoundIndex = 0; // tránh lỗi index
        }
        selectedSoundResId = soundResIds[selectedSoundIndex];


        if (auth.getCurrentUser() == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

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
                startActivity(new Intent(this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });

        waterProgressBar = findViewById(R.id.waterProgressBar);
        waterStatusText = findViewById(R.id.waterStatusText);
        addWaterButton = findViewById(R.id.addWaterButton);
        waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);

        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        waterHistoryAdapter = new WaterHistoryAdapter();
        waterHistoryRecyclerView.setAdapter(waterHistoryAdapter);

        setupBottomNavigation();
        fetchWaterGoal();
        fetchWaterHistory();

        addWaterButton.setOnClickListener(v -> showAddWaterDialog());
    }

    private void fetchWaterGoal() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double goal = document.getDouble("waterGoal");
                        waterGoal = goal != null ? goal : 2000;
                    } else {
                        waterGoal = 2000;
                    }
                    updateWaterStatus();
                })
                .addOnFailureListener(e -> {
                    waterGoal = 2000;
                    updateWaterStatus();
                });
    }

    private void fetchWaterHistory() {
        String userId = auth.getCurrentUser().getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        long startOfDay = calendar.getTimeInMillis();
        long endOfDay = startOfDay + 24 * 60 * 60 * 1000 - 1;

        db.collection("users").document(userId).collection("waterHistory")
                .whereGreaterThanOrEqualTo("timestamp", startOfDay)
                .whereLessThanOrEqualTo("timestamp", endOfDay)
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
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
                    drankWater += amount;
                    waterEntries.add(0, new WaterHistoryAdapter.WaterEntry(drinkType, amount, System.currentTimeMillis()));
                    waterHistoryAdapter.setWaterEntries(waterEntries);
                    updateWaterStatus();
                    Toast.makeText(this, "Đã thêm " + amount + "ml " + drinkType, Toast.LENGTH_SHORT).show();

                    SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
                    boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true); // mặc định bật

                    if (isSoundEnabled) {
                        MediaPlayer mediaPlayer = MediaPlayer.create(this, selectedSoundResId);
                        mediaPlayer.start();
                        mediaPlayer.setOnCompletionListener(MediaPlayer::release);
                    }


                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateWaterStatus() {
        // Tính toán lượng nước đã uống và còn thiếu
        String status = String.format("Đã uống: %.0f ml / Còn thiếu: %.0f ml", drankWater, Math.max(0, waterGoal - drankWater));
        waterStatusText.setText(status);

        // Cập nhật tiến trình thanh ProgressBar
        int progress = (int) ((drankWater / waterGoal) * 100);
        waterProgressBar.setProgress(Math.min(progress, 100));

        // Kiểm tra nếu người dùng đã uống đủ nước trong ngày
        if (drankWater >= waterGoal) {
            // Hiển thị thông báo Toast khi người dùng đạt mục tiêu
            Toast.makeText(this, "Chúc mừng! Bạn đã uống đủ nước hôm nay!", Toast.LENGTH_LONG).show();
        }
    }


    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }
}