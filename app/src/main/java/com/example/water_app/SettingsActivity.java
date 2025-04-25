package com.example.water_app;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.appcompat.widget.Toolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import java.io.IOException;

public class SettingsActivity extends BaseActivity {

    private TextView selectedSoundName;
    private final String[] soundOptions = {"Âm thanh 1", "Âm thanh 2", "Âm thanh 3", "Âm thanh tùy chỉnh"};
    private MediaPlayer mediaPlayer;
    private SharedPreferences prefs;
    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        // Initialize SharedPreferences
        prefs = getSharedPreferences("settings", MODE_PRIVATE);

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

        // Switch âm thanh
        SwitchCompat soundSwitch = findViewById(R.id.soundSwitch);
        soundSwitch.setChecked(prefs.getBoolean("sound_enabled", true));
        soundSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("sound_enabled", isChecked).apply();
        });

        // Hiển thị tên âm thanh đã chọn
        selectedSoundName = findViewById(R.id.selectedSoundName);
        int selectedIndex = prefs.getInt("selected_sound", 0);
        if (selectedIndex >= 0 && selectedIndex < soundOptions.length) {
            selectedSoundName.setText(soundOptions[selectedIndex]);
        }

        // Gắn sự kiện click cho container chọn âm thanh
        View soundOptionContainer = findViewById(R.id.soundOptionContainer1);
        soundOptionContainer.setOnClickListener(this::onSelectSoundClick);

        // Gắn sự kiện click cho nút nhập âm thanh
        Button importSoundButton = findViewById(R.id.importSoundButton);
        importSoundButton.setOnClickListener(v -> openFilePicker());

        // Initialize file picker launcher
        filePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                Uri uri = result.getData().getData();
                if (uri != null) {
                    // Store the custom sound URI
                    prefs.edit().putString("custom_sound_uri", uri.toString()).apply();
                    // Update selected sound to custom
                    prefs.edit().putInt("selected_sound", 3).apply();
                    selectedSoundName.setText(soundOptions[3]);
                    Toast.makeText(this, "Đã chọn âm thanh tùy chỉnh", Toast.LENGTH_SHORT).show();
                    // Play the selected custom sound
                    playSelectedSound(3);
                }
            }
        });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Chọn file âm thanh"));
    }

    public void onSelectSoundClick(View view) {
        int selectedIndex = prefs.getInt("selected_sound", 0);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn âm thanh nhắc nhở")
                .setSingleChoiceItems(soundOptions, selectedIndex, (dialog, which) -> {
                    prefs.edit().putInt("selected_sound", which).apply();
                    selectedSoundName.setText(soundOptions[which]);
                    // Phát âm thanh ngay khi chọn
                    playSelectedSound(which);
                })
                .setPositiveButton("OK", (dialog, which) -> dialog.dismiss())
                .setNegativeButton("Hủy", (dialog, which) -> dialog.dismiss());

        builder.create().show();
    }

    private void playSelectedSound(int soundIndex) {
        // Dừng âm thanh cũ nếu có
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }

        try {
            if (soundIndex == 3) {
                // Play custom sound
                String customSoundUri = prefs.getString("custom_sound_uri", null);
                if (customSoundUri != null) {
                    mediaPlayer = new MediaPlayer();
                    mediaPlayer.setDataSource(this, Uri.parse(customSoundUri));
                    mediaPlayer.prepare();
                    mediaPlayer.start();
                    Toast.makeText(this, "Đang phát: Âm thanh tùy chỉnh", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Chưa chọn âm thanh tùy chỉnh", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // Play predefined sounds
                int soundResId;
                switch (soundIndex) {
                    case 0:
                        soundResId = R.raw.sound4;
                        break;
                    case 1:
                        soundResId = R.raw.sound5;
                        break;
                    case 2:
                        soundResId = R.raw.sound6;
                        break;
                    default:
                        return;
                }
                mediaPlayer = MediaPlayer.create(this, soundResId);
                mediaPlayer.start();
                Toast.makeText(this, "Đang phát: " + soundOptions[soundIndex], Toast.LENGTH_SHORT).show();
            }

            // Stop and release after 3 seconds
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.stop();
                    mediaPlayer.release();
                    mediaPlayer = null;
                }
            }, 3000);

        } catch (IOException e) {
            Toast.makeText(this, "Lỗi khi phát âm thanh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release MediaPlayer when activity is destroyed
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }

    @Override
    protected int getSelectedNavItemId() {
        return 0;
    }
}