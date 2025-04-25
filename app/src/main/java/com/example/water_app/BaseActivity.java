package com.example.water_app;

import android.Manifest;
import android.app.AlertDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.bumptech.glide.Glide;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;
    private BroadcastReceiver notificationGifReceiver;
    private AlertDialog notificationDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }

        // Register BroadcastReceiver for notification GIF
        notificationGifReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                Log.d("BaseActivity", "Received GIF broadcast in " + getClass().getSimpleName());
                showNotificationGifDialog();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(
                notificationGifReceiver,
                new IntentFilter(ReminderReceiver.ACTION_SHOW_NOTIFICATION_GIF)
        );
        Log.d("BaseActivity", "Registered BroadcastReceiver in " + getClass().getSimpleName());
    }

    protected void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            updateNavigationSelection();

            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    if (!this.getClass().equals(MainActivity.class)) {
                        Intent intent = new Intent(this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                    return true;
                } else if (itemId == R.id.nav_statistics) {
                    if (!this.getClass().equals(StatisticsActivity.class)) {
                        Intent intent = new Intent(this, StatisticsActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                    return true;

                } else if (itemId == R.id.nav_set_reminder) {
                    if (!this.getClass().equals(ReminderActivity.class)) {
                        Intent intent = new Intent(this, ReminderActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        startActivity(intent);
                        finish();
                    }
                    return true;
                }
                return false;
            });
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == NOTIFICATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, "Quyền gửi thông báo bị từ chối", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showNotificationGifDialog() {
        Log.d("BaseActivity", "Attempting to show GIF dialog in " + getClass().getSimpleName());

        // Dismiss any existing dialog
        if (notificationDialog != null && notificationDialog.isShowing()) {
            notificationDialog.dismiss();
            Log.d("BaseActivity", "Dismissed existing dialog");
        }

        // Create dialog
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_notification_gif, null);
        builder.setView(dialogView);

        ImageView gifImageView = dialogView.findViewById(R.id.notification_gif_image);
        TextView gifTextView = dialogView.findViewById(R.id.notification_gif_text);

        if (gifImageView == null || gifTextView == null) {
            Log.e("BaseActivity", "Dialog views not found: gifImageView=" + gifImageView + ", gifTextView=" + gifTextView);
            Toast.makeText(this, "Lỗi: Không tìm thấy layout dialog", Toast.LENGTH_SHORT).show();
            return;
        }

        // Load GIF with Glide
        try {
            Glide.with(this)
                    .load(R.drawable.notification)
                    .error(R.drawable.congratulation_gif)
                    .into(gifImageView);
            gifTextView.setText("Đã đến giờ uống nước!");
            Log.d("BaseActivity", "GIF loaded successfully");
        } catch (Exception e) {
            Log.e("BaseActivity", "Error loading GIF: " + e.getMessage());
            Toast.makeText(this, "Lỗi khi tải GIF: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Create and show dialog
        notificationDialog = builder.create();
        notificationDialog.setCanceledOnTouchOutside(false);
        notificationDialog.setCancelable(false);
        notificationDialog.show();
        Log.d("BaseActivity", "Dialog shown");

        // Navigate to MainActivity when dialog content is clicked
        dialogView.setOnClickListener(v -> {
            Log.d("BaseActivity", "Dialog clicked, navigating to MainActivity");
            notificationDialog.dismiss();
            Intent intent = new Intent(this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivity(intent);
            if (!this.getClass().equals(MainActivity.class)) {
                finish();
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister BroadcastReceiver to prevent leaks
        if (notificationGifReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(notificationGifReceiver);
            Log.d("BaseActivity", "Unregistered BroadcastReceiver");
        }
        // Dismiss dialog to prevent window leaks
        if (notificationDialog != null && notificationDialog.isShowing()) {
            notificationDialog.dismiss();
            Log.d("BaseActivity", "Dismissed dialog in onDestroy");
        }
    }

    // Abstract method to let child activities specify their corresponding menu item
    protected abstract int getSelectedNavItemId();

    // Update the selected item in BottomNavigationView
    private void updateNavigationSelection() {
        int selectedItemId = getSelectedNavItemId();
        if (selectedItemId != 0) {
            bottomNavigationView.setSelectedItemId(selectedItemId);
        }
    }
}