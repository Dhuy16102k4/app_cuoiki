package com.example.water_app;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import androidx.appcompat.app.AppCompatActivity;
import com.example.water_app.auth.LoginActivity;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SplashActivity extends AppCompatActivity {
    private FirebaseAuth auth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        auth = FirebaseAuth.getInstance();

        // Delay 2 giây để hiển thị splash screen (tùy chọn)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                checkLoginStatus();
            }
        }, 4000);
    }

    private void checkLoginStatus() {
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            // Người dùng đã đăng nhập, chuyển đến MainActivity
            startActivity(new Intent(SplashActivity.this, MainActivity.class));
        } else {
            // Người dùng chưa đăng nhập, chuyển đến LoginActivity
            startActivity(new Intent(SplashActivity.this, LoginActivity.class));
        }
        finish(); // Đóng SplashActivity để không quay lại
    }
}