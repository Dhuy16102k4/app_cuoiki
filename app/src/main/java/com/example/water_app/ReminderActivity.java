package com.example.water_app;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Calendar;

public class ReminderActivity extends BaseActivity {

    private EditText intervalInput;
    private TextView timeSelectedText;
    private AlarmManager alarmManager;
    private PendingIntent pendingIntent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder);
        setupBottomNavigation();

        Button setReminderButton = findViewById(R.id.setReminderButton);
        intervalInput = findViewById(R.id.intervalInput);
        timeSelectedText = findViewById(R.id.timeSelectedText);

        alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);

        setReminderButton.setOnClickListener(v -> {
            String intervalText = intervalInput.getText().toString();
            if (intervalText.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập khoảng thời gian", Toast.LENGTH_SHORT).show();
                return;
            }

            int intervalSeconds = Integer.parseInt(intervalText);

            if (intervalSeconds <= 0) {
                Toast.makeText(this, "Khoảng thời gian phải lớn hơn 0", Toast.LENGTH_SHORT).show();
                return;
            }

            timeSelectedText.setText("Nhắc nhở sau " + intervalSeconds + " giây");

            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.SECOND, intervalSeconds);

            Intent intent = new Intent(ReminderActivity.this, ReminderReceiver.class);
            pendingIntent = PendingIntent.getBroadcast(ReminderActivity.this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

            alarmManager.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);
        });
    }

    @Override
    protected int getSelectedNavItemId() {
        return R.id.nav_set_reminder;
    }
}


