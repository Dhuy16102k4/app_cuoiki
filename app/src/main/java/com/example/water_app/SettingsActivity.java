package com.example.water_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SwitchCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsActivity extends AppCompatActivity {

    private TextView selectedSoundName;
    private final String[] soundOptions = {"Âm thanh 1", "Âm thanh 2", "Âm thanh 3"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitle("Cài đặt");
        setSupportActionBar(toolbar);

        // BottomNavigationView
        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigationView);
        bottomNav.setSelectedItemId(R.id.menu_settings);
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
                return true;
            }
            return false;
        });

        // Switch chế độ tối/sáng
        SwitchCompat themeSwitch = findViewById(R.id.themeSwitch);
        themeSwitch.setChecked(isDarkModeEnabled());
        themeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            AppCompatDelegate.setDefaultNightMode(
                    isChecked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );
        });

        // Switch âm thanh
        SwitchCompat soundSwitch = findViewById(R.id.soundSwitch);
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        soundSwitch.setChecked(prefs.getBoolean("sound_enabled", true));
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sound_enabled", isChecked).apply();
        });

        // Hiển thị tên âm thanh đã chọn
        selectedSoundName = findViewById(R.id.selectedSoundName);
        int selectedIndex = prefs.getInt("selected_sound", -1);
        if (selectedIndex >= 0 && selectedIndex < soundOptions.length) {
            selectedSoundName.setText(soundOptions[selectedIndex]);
        }

        // Gắn sự kiện click cho toàn bộ container chọn âm thanh
        View soundOptionContainer = findViewById(R.id.soundOptionContainer1);
        soundOptionContainer.setOnClickListener(this::onSelectSoundClick);
    }

    private boolean isDarkModeEnabled() {
        int nightMode = AppCompatDelegate.getDefaultNightMode();
        return nightMode == AppCompatDelegate.MODE_NIGHT_YES;
    }

    public void onSelectSoundClick(View view) {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        int selectedIndex = prefs.getInt("selected_sound", -1);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn âm thanh nhắc nhở")
                .setSingleChoiceItems(soundOptions, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt("selected_sound", which).apply();
                    selectedSoundName.setText(soundOptions[which]);
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }
}







