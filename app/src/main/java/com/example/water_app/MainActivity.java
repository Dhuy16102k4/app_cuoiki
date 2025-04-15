package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water_app.auth.EditProfileActivity;
import com.example.water_app.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private ProgressBar waterProgressBar;
    private TextView waterStatusText;
    private Button addWaterButton;
    private RecyclerView waterHistoryRecyclerView;
    private double waterGoal = 0; // Lượng nước cần uống (ml)
    private double drankWater = 0; // Lượng nước đã uống (ml)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Khởi tạo Firebase
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
                Toast.makeText(MainActivity.this, "Mở màn hình cài đặt", Toast.LENGTH_SHORT).show();
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

        // Khởi tạo các thành phần giao diện
        waterProgressBar = findViewById(R.id.waterProgressBar);
        waterStatusText = findViewById(R.id.waterStatusText);
        addWaterButton = findViewById(R.id.addWaterButton);
        waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);

        // Thiết lập RecyclerView
        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Fetch dữ liệu từ Firestore
        fetchWaterGoal();

        // Xử lý nút Thêm nước
        addWaterButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Mở dialog nhập lượng nước", Toast.LENGTH_SHORT).show();
            // Sau này bạn có thể thêm dialog để nhập drankWater và gọi updateWaterStatus()
        });

        // Thiết lập BottomNavigationView từ BaseActivity
        setupBottomNavigation();
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
                            waterGoal = 2000; // Giá trị mặc định nếu không có dữ liệu
                            updateWaterStatus();
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu người dùng", Toast.LENGTH_SHORT).show();
                        waterGoal = 2000; // Giá trị mặc định
                        updateWaterStatus();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi khi lấy dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    waterGoal = 2000; // Giá trị mặc định
                    updateWaterStatus();
                });
    }

    private void updateWaterStatus() {
        // Cập nhật TextView
        String status = String.format("Đã uống: %.0f ml / Còn thiếu: %.0f ml", drankWater, waterGoal - drankWater);
        waterStatusText.setText(status);

        // Cập nhật ProgressBar
        int progress = (int) ((drankWater / waterGoal) * 100);
        waterProgressBar.setProgress(Math.min(progress, 100));
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }
}