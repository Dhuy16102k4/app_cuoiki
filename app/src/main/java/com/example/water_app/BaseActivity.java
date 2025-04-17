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
        // Set the layout in the child activity before calling setContentView
    }

    protected void setupBottomNavigation() {
        bottomNavigationView = findViewById(R.id.bottomNavigationView);
        if (bottomNavigationView != null) {
            // Highlight the correct menu item based on the current activity
            updateNavigationSelection();

            // Set up navigation listener
            bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
                int itemId = item.getItemId();
                if (itemId == R.id.nav_home) {
                    // Check if the current activity is NOT MainActivity
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
                    if(!this.getClass().equals(ReminderActivity.class)) {
                        Intent intent = new Intent(this, ReminderActivity.class);
                        startActivity(intent);
                    }
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