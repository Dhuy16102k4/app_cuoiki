package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
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

public class MainActivity extends BaseActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        auth = FirebaseAuth.getInstance();

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

        // Khởi tạo các thành phần
        ProgressBar waterProgressBar = findViewById(R.id.waterProgressBar);
        TextView waterStatusText = findViewById(R.id.waterStatusText);
        Button addWaterButton = findViewById(R.id.addWaterButton);
        RecyclerView waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);

        // Thiết lập RecyclerView
        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Xử lý nút Thêm nước
        addWaterButton.setOnClickListener(v -> {
            Toast.makeText(MainActivity.this, "Mở dialog nhập lượng nước", Toast.LENGTH_SHORT).show();
        });

        // Thiết lập BottomNavigationView từ BaseActivity
        setupBottomNavigation();
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_home;
    }
}