package com.example.water_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

public class ReminderActivity extends BaseActivity {

    private SeekBar intervalSeekBar;
    private TextView intervalText;
    private TimePicker startTimePicker, endTimePicker; // Thêm TimePicker cho giờ bắt đầu và kết thúc
    private TextView reminderStatusText;
    private Button toggleReminderButton;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private SharedPreferences preferences;
    private int selectedInterval = 60;
    private int startHour = 8, startMinute = 0;
    private int endHour = 20, endMinute = 0;
    private boolean isReminderActive = false;
    private final int[] intervalValues = {15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        setupBottomNavigation();

        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalText = findViewById(R.id.intervalText);
        startTimePicker = findViewById(R.id.startTimePicker); // Khởi tạo TimePicker cho giờ bắt đầu
        endTimePicker = findViewById(R.id.endTimePicker); // Khởi tạo TimePicker cho giờ kết thúc
        reminderStatusText = findViewById(R.id.reminderStatusText);
        toggleReminderButton = findViewById(R.id.toggleReminderButton);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        preferences = getSharedPreferences("ReminderPrefs", MODE_PRIVATE);

        loadPreferences();

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        setupIntervalSeekBar();

        // Đặt thời gian mặc định cho TimePicker từ SharedPreferences
        startTimePicker.setHour(startHour);
        startTimePicker.setMinute(startMinute);
        endTimePicker.setHour(endHour);
        endTimePicker.setMinute(endMinute);

        // Lắng nghe thay đổi giờ và phút của TimePicker
        startTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            startHour = hourOfDay;
            startMinute = minute;
            updateTimeButtons();
            updateReminderStatus();
            savePreferences();
        });

        endTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            endHour = hourOfDay;
            endMinute = minute;
            updateTimeButtons();
            updateReminderStatus();
            savePreferences();
        });

        toggleReminderButton.setOnClickListener(v -> {
            if (isReminderActive) {
                cancelReminder();
            } else {
                startReminder();
            }
        });

        updateIntervalText();
        updateReminderStatus();

        // Điều chỉnh âm lượng hệ thống (đặt âm lượng hệ thống ở mức tối đa)
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0);

        // Kiểm tra khi đến giờ nhắc nhở, phát âm thanh
        playReminderSound();
    }

    private void setupIntervalSeekBar() {
        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedInterval = intervalValues[progress];
                updateIntervalText();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                savePreferences();
                updateReminderStatus();
            }
        });

        for (int i = 0; i < intervalValues.length; i++) {
            if (intervalValues[i] == selectedInterval) {
                intervalSeekBar.setProgress(i);
                break;
            }
        }
    }

    private void updateIntervalText() {
        intervalText.setText(selectedInterval + " phút");
    }

    private void loadPreferences() {
        selectedInterval = preferences.getInt("interval", 60);
        startHour = preferences.getInt("startHour", 8);
        startMinute = preferences.getInt("startMinute", 0);
        endHour = preferences.getInt("endHour", 20);
        endMinute = preferences.getInt("endMinute", 0);
        isReminderActive = preferences.getBoolean("isReminderActive", false);
    }

    private void savePreferences() {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("interval", selectedInterval);
        editor.putInt("startHour", startHour);
        editor.putInt("startMinute", startMinute);
        editor.putInt("endHour", endHour);
        editor.putInt("endMinute", endMinute);
        editor.putBoolean("isReminderActive", isReminderActive);
        editor.apply();
    }

    private void updateTimeButtons() {
        // Cập nhật các button hiển thị giờ bắt đầu và kết thúc
        reminderStatusText.setText(String.format(
                "Nhắc nhở mỗi %d phút từ %02d:%02d đến %02d:%02d",
                selectedInterval, startHour, startMinute, endHour, endMinute
        ));
    }

    private void updateReminderStatus() {
        if (isReminderActive) {
            toggleReminderButton.setText("Tắt thông báo");
        } else {
            toggleReminderButton.setText("Bật thông báo");
        }
    }

    private void startReminder() {
        Calendar startCalendar = Calendar.getInstance();
        startCalendar.set(Calendar.HOUR_OF_DAY, startHour);
        startCalendar.set(Calendar.MINUTE, startMinute);
        startCalendar.set(Calendar.SECOND, 0);
        startCalendar.set(Calendar.MILLISECOND, 0);

        Calendar endCalendar = Calendar.getInstance();
        endCalendar.set(Calendar.HOUR_OF_DAY, endHour);
        endCalendar.set(Calendar.MINUTE, endMinute);
        endCalendar.set(Calendar.SECOND, 0);
        endCalendar.set(Calendar.MILLISECOND, 0);

        if (endCalendar.getTimeInMillis() <= startCalendar.getTimeInMillis()) {
            Toast.makeText(this, "Giờ kết thúc phải sau giờ bắt đầu", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ReminderReceiver.class);
        pendingIntent = PendingIntent.getBroadcast(
                this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        long intervalMillis = selectedInterval * 60 * 1000;
        long triggerTime = startCalendar.getTimeInMillis();
        if (System.currentTimeMillis() > triggerTime) {
            triggerTime += intervalMillis;
        }

        alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP,
                triggerTime,
                intervalMillis,
                pendingIntent
        );

        isReminderActive = true;
        updateReminderStatus();
        savePreferences();
        Toast.makeText(this, "Đã bật thông báo", Toast.LENGTH_SHORT).show();
    }

    private void cancelReminder() {
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
        isReminderActive = false;
        updateReminderStatus();
        savePreferences();
        Toast.makeText(this, "Đã tắt thông báo", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_set_reminder;
    }

    private void playReminderSound() {
        // Lấy SharedPreferences để kiểm tra xem âm thanh có được bật và loại âm thanh đã chọn
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);  // Mặc định là bật âm thanh
        int selectedSound = prefs.getInt("selected_sound", 0);  // Mặc định là âm thanh 1

        if (isSoundEnabled) {
            int soundResId = R.raw.sound1;  // Mặc định âm thanh 1

            // Lựa chọn âm thanh dựa trên giá trị index người dùng đã chọn
            switch (selectedSound) {
                case 1:
                    soundResId = R.raw.sound2;
                    break;
                case 2:
                    soundResId = R.raw.sound3;
                    break;
            }

            // Phát âm thanh
            MediaPlayer mediaPlayer = MediaPlayer.create(this, soundResId);

            // Điều chỉnh âm lượng (1.0 là âm lượng tối đa)
            mediaPlayer.setVolume(1.0f, 1.0f); // Âm lượng trái và phải đều là 1.0 (tối đa)

            mediaPlayer.start();
        }
    }
}
