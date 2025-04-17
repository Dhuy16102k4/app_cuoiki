package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import com.example.water_app.auth.EditProfileActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import android.widget.Toast;

public abstract class BaseActivity extends AppCompatActivity {

    protected BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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
                    Toast.makeText(this, "Thống kê", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to StatisticsActivity when implemented
                    return true;
                } else if (itemId == R.id.nav_set_reminder) {
                    Toast.makeText(this, "Mở cài đặt thời gian nhắc uống nước", Toast.LENGTH_SHORT).show();
                    // TODO: Navigate to ReminderActivity when implemented
                    return true;
                }
                return false;
            });
        }
    }

    // Abstract method to let child activities specify their corresponding menu item
    protected abstract int getSelectedNavItemId();

    // Update the selected item in BottomNavigationView
    private void updateNavigationSelection() {
        int selectedItemId = getSelectedNavItemId();
        if (selectedItemId != 0) { // Only set if a valid ID is provided
            bottomNavigationView.setSelectedItemId(selectedItemId);
        }
    }
}