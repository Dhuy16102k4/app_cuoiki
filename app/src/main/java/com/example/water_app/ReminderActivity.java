package com.example.water_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import java.util.Calendar;

public class ReminderActivity extends BaseActivity {

    private SeekBar intervalSeekBar;
    private TextView intervalText;
    private Button startTimeButton, endTimeButton;
    private TextView reminderStatusText;
    private Button toggleReminderButton;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private SharedPreferences preferences;
    private int selectedInterval = 60; // Mặc định 60 phút
    private int startHour = 8, startMinute = 0; // Mặc định 8:00
    private int endHour = 20, endMinute = 0; // Mặc định 20:00
    private boolean isReminderActive = false;
    private final int[] intervalValues = {15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        setupBottomNavigation();

        // Khởi tạo
        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalText = findViewById(R.id.intervalText);
        startTimeButton = findViewById(R.id.startTimeButton);
        endTimeButton = findViewById(R.id.endTimeButton);
        reminderStatusText = findViewById(R.id.reminderStatusText);
        toggleReminderButton = findViewById(R.id.toggleReminderButton);
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        preferences = getSharedPreferences("ReminderPrefs", MODE_PRIVATE);

        // Load cài đặt từ SharedPreferences
        loadPreferences();

        // Thiết lập Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Thiết lập SeekBar
        setupIntervalSeekBar();

        // Xử lý chọn khung giờ
        startTimeButton.setOnClickListener(v -> showTimePicker(true));
        endTimeButton.setOnClickListener(v -> showTimePicker(false));

        // Xử lý bật/tắt nhắc nhở
        toggleReminderButton.setOnClickListener(v -> {
            if (isReminderActive) {
                cancelReminder();
            } else {
                startReminder();
            }
        });

        // Cập nhật giao diện ban đầu
        updateIntervalText();
        updateTimeButtons();
        updateReminderStatus();
    }

    private void setupIntervalSeekBar() {
        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedInterval = intervalValues[progress];
                updateIntervalText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                savePreferences();
                updateReminderStatus();
            }
        });

        // Đặt giá trị mặc định cho SeekBar
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

    private void showTimePicker(boolean isStartTime) {
        Calendar calendar = Calendar.getInstance();
        int hour = isStartTime ? startHour : endHour;
        int minute = isStartTime ? startMinute : endMinute;

        TimePickerDialog timePickerDialog = new TimePickerDialog(
                this,
                (view, hourOfDay, minuteOfHour) -> {
                    if (isStartTime) {
                        startHour = hourOfDay;
                        startMinute = minuteOfHour;
                    } else {
                        endHour = hourOfDay;
                        endMinute = minuteOfHour;
                    }
                    updateTimeButtons();
                    updateReminderStatus();
                    savePreferences();
                },
                hour,
                minute,
                true
        );
        timePickerDialog.show();
    }

    private void updateTimeButtons() {
        startTimeButton.setText(String.format("%02d:%02d", startHour, startMinute));
        endTimeButton.setText(String.format("%02d:%02d", endHour, endMinute));
    }

    private void updateReminderStatus() {
        if (isReminderActive) {
            reminderStatusText.setText(String.format(
                    "Nhắc nhở mỗi %d phút từ %02d:%02d đến %02d:%02d",
                    selectedInterval, startHour, startMinute, endHour, endMinute
            ));
            toggleReminderButton.setText("Tắt nhắc nhở");
        } else {
            reminderStatusText.setText("Chưa bật nhắc nhở");
            toggleReminderButton.setText("Bật nhắc nhở");
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
        Toast.makeText(this, "Đã bật nhắc nhở", Toast.LENGTH_SHORT).show();
    }

    private void cancelReminder() {
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
        }
        isReminderActive = false;
        updateReminderStatus();
        savePreferences();
        Toast.makeText(this, "Đã tắt nhắc nhở", Toast.LENGTH_SHORT).show();
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_set_reminder;
    }
}