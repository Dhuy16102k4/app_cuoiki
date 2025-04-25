package com.example.water_app;

import android.annotation.SuppressLint;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;

import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.util.Calendar;

public class ReminderReceiver extends BroadcastReceiver {

    public static final String ACTION_SHOW_NOTIFICATION_GIF = "com.example.water_app.SHOW_NOTIFICATION_GIF";

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

        // Play the selected sound
        playReminderSound(context);

        // Create and show notification with PendingIntent
        createNotificationChannel(context);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "reminder_channel")
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Nhắc nhở uống nước")
                .setContentText("Đến lúc uống nước rồi!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent);

        NotificationManagerCompat notificationManager = NotificationManagerCompat.from(context);
        notificationManager.notify(1, builder.build());

        // Send broadcast to show GIF
        Intent gifIntent = new Intent(ACTION_SHOW_NOTIFICATION_GIF);
        LocalBroadcastManager.getInstance(context).sendBroadcast(gifIntent);
    }

    private void playReminderSound(Context context) {
        SharedPreferences prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE);
        boolean isSoundEnabled = prefs.getBoolean("sound_enabled", true);
        int selectedSound = prefs.getInt("selected_sound", 0);

        if (isSoundEnabled) {
            MediaPlayer mediaPlayer = null;
            try {
                if (selectedSound == 3) {
                    // Play custom sound
                    String customSoundUri = prefs.getString("custom_sound_uri", null);
                    if (customSoundUri != null) {
                        mediaPlayer = new MediaPlayer();
                        mediaPlayer.setDataSource(context, Uri.parse(customSoundUri));
                        mediaPlayer.prepare();
                        mediaPlayer.start();
                    }
                } else {
                    // Play predefined sounds
                    int soundResId;
                    switch (selectedSound) {
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
                    mediaPlayer = MediaPlayer.create(context, soundResId);
                    mediaPlayer.start();
                }

                // Ensure MediaPlayer is released after playing
                MediaPlayer finalMediaPlayer = mediaPlayer;
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    if (finalMediaPlayer != null) {
                        finalMediaPlayer.stop();
                        finalMediaPlayer.release();
                    }
                }, 3000);

            } catch (IOException e) {
                // Handle error silently in receiver to avoid crashing
            }
        }
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