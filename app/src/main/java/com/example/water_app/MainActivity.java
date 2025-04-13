package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.water_app.auth.EditProfileActivity;
import com.example.water_app.auth.LoginActivity; // Activity đăng nhập`
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;

public class MainActivity extends AppCompatActivity {
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
        toolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.menu_profile) {
                    // Mở màn hình thông tin cá nhân
                    startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
                    return true;
                } else if (itemId == R.id.menu_settings) {
                    // Mở màn hình cài đặt (có thể thêm sau)
                    Toast.makeText(MainActivity.this, "Mở màn hình cài đặt", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.menu_logout) {
                    // Đăng xuất
                    auth.signOut();
                    Toast.makeText(MainActivity.this, "Đã đăng xuất", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(MainActivity.this, LoginActivity.class));
                    finish();
                    return true;
                }
                return false;
            }
        });

        // Khởi tạo các thành phần
        ProgressBar waterProgressBar = findViewById(R.id.waterProgressBar);
        TextView waterStatusText = findViewById(R.id.waterStatusText);
        Button addWaterButton = findViewById(R.id.addWaterButton);
        RecyclerView waterHistoryRecyclerView = findViewById(R.id.waterHistoryRecyclerView);
        BottomNavigationView bottomNavigationView = findViewById(R.id.bottomNavigationView);

        // Thiết lập RecyclerView
        waterHistoryRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Xử lý nút Thêm nước
        addWaterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "Mở dialog nhập lượng nước", Toast.LENGTH_SHORT).show();
            }
        });

        // Xử lý BottomNavigationView
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    Toast.makeText(MainActivity.this, "Trang chủ", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_statistics) {
                    Toast.makeText(MainActivity.this, "Thống kê", Toast.LENGTH_SHORT).show();
                    return true;
                } else if (itemId == R.id.nav_set_reminder) {
                    Toast.makeText(MainActivity.this, "Mở cài đặt thời gian nhắc uống nước", Toast.LENGTH_SHORT).show();
                    // Sau này có thể thay bằng Intent hoặc Dialog
                    return true;
                }
                return false;
            }
        });
    }
}