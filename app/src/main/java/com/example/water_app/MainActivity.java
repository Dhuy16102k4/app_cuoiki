package com.example.water_app;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water_app.auth.EditProfileActivity;
import com.example.water_app.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends BaseActivity {
    private static final String TAG = "MainActivity";
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Kiểm tra đăng nhập
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Water Reminder");
        toolbar.setTitleTextColor(getResources().getColor(android.R.color.white));
        toolbar.inflateMenu(R.menu.main_menu);
        toolbar.setOnMenuItemClickListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.menu_profile) {
                startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
                return true;
            } else if (itemId == R.id.menu_settings) {
                startActivity(new Intent(MainActivity.this, SettingsActivity.class));
                return true;
            } else if (itemId == R.id.menu_logout) {
                auth.signOut();
                Toast.makeText(MainActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(MainActivity.this, LoginActivity.class));
                finish();
                return true;
            }
            return false;
        });

        // Khởi tạo views
        waterProgressBar = findViewById(R.id.waterProgressBar);
        waterStatusText = findViewById(R.id.waterStatusText);
        addWaterButton = findViewById(R.id.addWaterButton);
        waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);

        // Thiết lập RecyclerView
        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        waterHistoryAdapter = new WaterHistoryAdapter();
        waterHistoryRecyclerView.setAdapter(waterHistoryAdapter);

        // Thiết lập BottomNavigationView
        setupBottomNavigation();

        // Lấy dữ liệu
        fetchWaterGoal();
        fetchWaterHistory();

        // Xử lý sự kiện thêm nước
        addWaterButton.setOnClickListener(v -> showAddWaterDialog());
    }

    private void fetchWaterGoal() {
        String userId = auth.getCurrentUser().getUid();
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        Double goal = document.getDouble("waterGoal");
                        if (goal != null) {
                            waterGoal = goal;
                            updateWaterStatus();
                        } else {
                            Toast.makeText(this, "Không tìm thấy mục tiêu nước", Toast.LENGTH_SHORT).show();
                            waterGoal = 2000;
                            updateWaterStatus();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                        waterGoal = 2000;
                        updateWaterStatus();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lấy dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    waterGoal = 2000;
                    updateWaterStatus();
                });
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lấy lịch sử: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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

            CardView selectedCard = (CardView) v;
            selectedCard.setCardBackgroundColor(getResources().getColor(R.color.card_selected, null));

            if (v.getId() == R.id.cardWater) {
                selectedDrinkType[0] = "Nước lọc";
            } else if (v.getId() == R.id.cardTea) {
                selectedDrinkType[0] = "Trà";
            } else if (v.getId() == R.id.cardCoffee) {
                selectedDrinkType[0] = "Cà phê";
            } else if (v.getId() == R.id.cardMilk) {
                selectedDrinkType[0] = "Sữa";
            }
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
            case "Trà":
                return amount * 0.9;
            case "Cà phê":
                return amount * 0.8;
            case "Sữa":
                return amount * 0.85;
            case "Nước lọc":
            default:
                return amount;
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
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lưu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void updateWaterStatus() {
        String status = String.format("Đã uống: %.0f ml / Còn thiếu: %.0f ml", drankWater, Math.max(0, waterGoal - drankWater));
        waterStatusText.setText(status);
        int progress = (int) ((drankWater / waterGoal) * 100);
        waterProgressBar.setProgress(Math.min(progress, 100));
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }
}