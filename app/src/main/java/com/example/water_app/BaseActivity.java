package com.example.water_app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.example.water_app.auth.EditProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;
    private static final int NOTIFICATION_PERMISSION_REQUEST_CODE = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Yêu cầu quyền POST_NOTIFICATIONS cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        NOTIFICATION_PERMISSION_REQUEST_CODE);
            }
        }
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
                    Toast.makeText(this, "Thống kê - Chưa triển khai", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to StatisticsActivity when implemented
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