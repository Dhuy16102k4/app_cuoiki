package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Cài đặt");
        setSupportActionBar(toolbar);

        // Setup BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_settings); // đánh dấu tab đang ở settings

        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(SettingsActivity.this, MainActivity.class));
                finish();
                return true;
            } else if (id == R.id.nav_set_reminder) {
                startActivity(new Intent(SettingsActivity.this, ReminderActivity.class));
                finish();
                return true;
            } else if (id == R.id.menu_settings) {
                // đang ở settings, không làm gì
                return true;
            }
            return false;
        });

        // Xử lý Switch để chuyển chế độ sáng/tối
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setChecked(isDarkModeEnabled()); // Set trạng thái theo chế độ hiện tại

        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // Bật chế độ tối
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
            } else {
                // Bật chế độ sáng
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
            }
        });
    }

    // Hàm kiểm tra chế độ hiện tại (sáng/tối)
    private boolean isDarkModeEnabled() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES;
    }
}


