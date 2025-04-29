package com.example.water_app;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.Button;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.appcompat.widget.Toolbar;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

public class ReminderActivity extends BaseActivity {

    private SeekBar intervalSeekBar;
    private TextView intervalText, reminderStatusText, currentDateText; // Added currentDateText
    private TimePicker startTimePicker, endTimePicker;
    private Button toggleReminderButton;

    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;
    private SharedPreferences preferences;

    private int selectedInterval = 60; // In seconds
    private int startHour = 8, startMinute = 0;
    private int endHour = 20, endMinute = 0;
    private boolean isReminderActive = false;

    private final int[] intervalValues = {15, 30, 45, 60, 75, 90, 105, 120, 135, 150, 165, 180}; // Seconds

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        setupBottomNavigation();

        // Ánh xạ view
        intervalSeekBar = findViewById(R.id.intervalSeekBar);
        intervalText = findViewById(R.id.intervalText);
        startTimePicker = findViewById(R.id.startTimePicker);
        endTimePicker = findViewById(R.id.endTimePicker);
        reminderStatusText = findViewById(R.id.reminderStatusText);
        toggleReminderButton = findViewById(R.id.toggleReminderButton);
        currentDateText = findViewById(R.id.currentDateText); // Initialize currentDateText
        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        preferences = getSharedPreferences("ReminderPrefs", MODE_PRIVATE);

        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Display current date
        displayCurrentDate();

        // Load dữ liệu lưu trữ
        loadPreferences();

        // Setup SeekBar
        setupIntervalSeekBar();

        // Set lại TimePicker từ dữ liệu
        startTimePicker.setHour(startHour);
        startTimePicker.setMinute(startMinute);
        endTimePicker.setHour(endHour);
        endTimePicker.setMinute(endMinute);

        // Bắt sự kiện thay đổi thời gian
        startTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            startHour = hourOfDay;
            startMinute = minute;
            updateReminderDescription();
            savePreferences();
        });

        endTimePicker.setOnTimeChangedListener((view, hourOfDay, minute) -> {
            endHour = hourOfDay;
            endMinute = minute;
            updateReminderDescription();
            savePreferences();
        });

        // Nút bật/tắt thông báo
        toggleReminderButton.setOnClickListener(v -> {
            if (isReminderActive) {
                cancelReminder();
            } else {
                startReminder();
            }
        });

        // Cập nhật UI theo trạng thái đã lưu
        updateIntervalText();
        updateReminderDescription();
        updateReminderStatus();

        // Cài đặt âm lượng hệ thống
        AudioManager audioManager = (AudioManager) getSystemService(AUDIO_SERVICE);
        audioManager.setStreamVolume(
                AudioManager.STREAM_MUSIC,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC),
                0
        );
    }

    private void displayCurrentDate() {
        // Get current date
        Date currentDate = new Date();
        // Format date as DD/MM/YYYY
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String formattedDate = dateFormat.format(currentDate);
        // Set the formatted date to the TextView
        currentDateText.setText(formattedDate);
    }

    private void setupIntervalSeekBar() {
        intervalSeekBar.setMax(intervalValues.length - 1);
        for (int i = 0; i < intervalValues.length; i++) {
            if (intervalValues[i] == selectedInterval) {
                intervalSeekBar.setProgress(i);
                break;
            }
        }

        intervalSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                selectedInterval = intervalValues[progress];
                updateIntervalText();
            }

            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {
                savePreferences();
                updateReminderDescription();
            }
        });
    }

    private void updateIntervalText() {
        intervalText.setText(selectedInterval + " giây");
    }

    private void updateReminderDescription() {
        reminderStatusText.setText(String.format(
                "Nhắc nhở mỗi %d giây từ %02d:%02d đến %02d:%02d",
                selectedInterval, startHour, startMinute, endHour, endMinute
        ));
    }

    private void updateReminderStatus() {
        if (isReminderActive) {
            toggleReminderButton.setText("Tắt thông báo");
            intervalSeekBar.setEnabled(false);
            startTimePicker.setEnabled(false);
            endTimePicker.setEnabled(false);
        } else {
            toggleReminderButton.setText("Bật thông báo");
            intervalSeekBar.setEnabled(true);
            startTimePicker.setEnabled(true);
            endTimePicker.setEnabled(true);
        }
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

        long intervalMillis = selectedInterval * 1000; // Convert seconds to milliseconds
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
}