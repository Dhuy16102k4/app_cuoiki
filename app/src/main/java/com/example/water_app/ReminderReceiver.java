package com.example.water_app;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    @SuppressLint("MissingPermission")
    @Override
    public void onReceive(Context context, Intent intent) {
        SharedPreferences preferences = context.getSharedPreferences("ReminderPrefs", Context.MODE_PRIVATE);
        int startHour = preferences.getInt("startHour", 8);
        int startMinute = preferences.getInt("startMinute", 0);
        int endHour = preferences.getInt("endHour", 20);
        int endMinute = preferences.getInt("endMinute", 0);

        Calendar now = Calendar.getInstance();
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

        long currentTime = now.getTimeInMillis();
        if (currentTime < startCalendar.getTimeInMillis() || currentTime > endCalendar.getTimeInMillis()) {
            return;
        }

        createNotificationChannel(context);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Nhắc nhở uống nước")
                .setContentText("Đến lúc uống nước rồi!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            String channelId = "reminder_channel";
            CharSequence name = "Nhắc nhở uống nước";
            String description = "Thông báo nhắc bạn uống nước";
            int importance = NotificationManager.IMPORTANCE_HIGH;

            NotificationChannel channel = new NotificationChannel(channelId, name, importance);
            channel.setDescription(description);

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
}