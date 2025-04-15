package com.example.water_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.water_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private TextInputEditText emailInput, passwordInput, confirmPasswordInput, weightInput, heightInput;
    private AutoCompleteTextView healthConditionSpinner;
    private MaterialButton signupButton;
    private TextView backToLoginText;
    private boolean isFormatting; // Flag to prevent recursive TextWatcher calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        healthConditionSpinner = findViewById(R.id.healthConditionSpinner);
        signupButton = findViewById(R.id.signupButton);
        backToLoginText = findViewById(R.id.backToLoginText);

        // Set up AutoCompleteTextView adapter
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.health_conditions, android.R.layout.simple_dropdown_item_1line);
        healthConditionSpinner.setAdapter(adapter);

        // Add TextWatcher for height input to auto-format
        heightInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
                // No action needed
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // No action needed
            }

            @Override
            public void afterTextChanged(Editable s) {
                if (isFormatting) return; // Prevent recursive calls

                isFormatting = true;
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    try {
                        // Remove any existing decimal points for raw integer input
                        String cleanInput = input.replace(".", "");
                        int rawHeight = Integer.parseInt(cleanInput);
                        if (rawHeight >= 50 && rawHeight <= 300) { // 50 cm to 300 cm
                            double formattedHeight = rawHeight / 100.0;
                            String formatted = String.format("%.2f", formattedHeight);
                            if (!input.equals(formatted)) {
                                heightInput.setText(formatted);
                                heightInput.setSelection(formatted.length());
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid input; validation will handle it later
                    }
                }
                isFormatting = false;
            }
        });

        signupButton.setOnClickListener(v -> {
            String email = emailInput.getText().toString().trim();
            String password = passwordInput.getText().toString().trim();
            String confirmPassword = confirmPasswordInput.getText().toString().trim();
            String weightStr = weightInput.getText().toString().trim();
            String heightStr = heightInput.getText().toString().trim();
            String healthCondition = healthConditionSpinner.getText().toString().trim();

            // Validate inputs
            if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                    weightStr.isEmpty() || heightStr.isEmpty() || healthCondition.isEmpty()) {
                Toast.makeText(SignupActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!password.equals(confirmPassword)) {
                Toast.makeText(SignupActivity.this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                return;
            }

            double weight;
            double height;
            try {
                weight = Double.parseDouble(weightStr);
                height = Double.parseDouble(heightStr);
                // Validate ranges
                if (weight < 20 || weight > 300) {
                    Toast.makeText(SignupActivity.this, "Cân nặng phải từ 20 kg đến 300 kg", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (height < 0.5 || height > 3.0) {
                    Toast.makeText(SignupActivity.this, "Chiều cao phải từ 0.5 m đến 3.0 m", Toast.LENGTH_SHORT).show();
                    return;
                }
            } catch (NumberFormatException e) {
                Toast.makeText(SignupActivity.this, "Cân nặng hoặc chiều cao không hợp lệ", Toast.LENGTH_SHORT).show();
                return;
            }

            // Calculate water goal
            double waterGoal = calculateWaterGoal(weight, height, healthCondition);

            auth.createUserWithEmailAndPassword(email, password)
                    .addOnCompleteListener(task -> {
                        if (task.isSuccessful() && auth.getCurrentUser() != null) {
                            String userId = auth.getCurrentUser().getUid();

                            // Create user data
                            Map<String, Object> user = new HashMap<>();
                            user.put("email", email);
                            user.put("weight", weight);
                            user.put("height", height);
                            user.put("healthCondition", healthCondition);
                            user.put("waterGoal", waterGoal);

                            // Save to Firestore
                            db.collection("users").document(userId).set(user)
                                    .addOnSuccessListener(aVoid -> {
                                        Toast.makeText(SignupActivity.this,
                                                "Đăng ký thành công! Mục tiêu nước: " + waterGoal + " ml",
                                                Toast.LENGTH_LONG).show();
                                        startActivity(new Intent(SignupActivity.this, LoginActivity.class));
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        Toast.makeText(SignupActivity.this, "Lưu thông tin thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                                    });
                        } else {
                            Toast.makeText(SignupActivity.this,
                                    "Đăng ký thất bại: " + (task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định"),
                                    Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        backToLoginText.setOnClickListener(v -> {
            startActivity(new Intent(SignupActivity.this, LoginActivity.class));
            finish();
        });
    }

    private double calculateWaterGoal(double weight, double height, String healthCondition) {
        double baseWater = weight * 33;
        double heightAdjustment = height * 500; // 500 ml per meter
        double healthAdjustment;
        switch (healthCondition) {
            case "Người tập gym":
                healthAdjustment = baseWater * 0.2;
                break;
            case "Phụ nữ mang thai":
                healthAdjustment = baseWater * 0.3;
                break;
            case "Người cao tuổi":
                healthAdjustment = -baseWater * 0.1;
                break;
            case "Thông thường":
            default:
                healthAdjustment = 0;
                break;
        }
        double totalWater = baseWater + heightAdjustment + healthAdjustment;
        return Math.max(1500, Math.min(4000, totalWater));
    }
}