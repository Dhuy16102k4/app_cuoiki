package com.example.water_app.auth;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.example.water_app.R;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class SignupActivity extends AppCompatActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;

    private EditText emailInput, passwordInput, confirmPasswordInput, weightInput, heightInput;
    private Spinner healthConditionSpinner;
    private Button signupButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Khởi tạo các view
        emailInput = findViewById(R.id.emailInput);
        passwordInput = findViewById(R.id.passwordInput);
        confirmPasswordInput = findViewById(R.id.confirmPasswordInput);
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        healthConditionSpinner = findViewById(R.id.healthConditionSpinner);
        signupButton = findViewById(R.id.signupButton);

        // Thiết lập Spinner
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this,
                R.array.health_conditions, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        healthConditionSpinner.setAdapter(adapter);

        signupButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String password = passwordInput.getText().toString().trim();
                String confirmPassword = confirmPasswordInput.getText().toString().trim();
                String weightStr = weightInput.getText().toString().trim();
                String heightStr = heightInput.getText().toString().trim();
                String healthCondition = healthConditionSpinner.getSelectedItem().toString();

                // Kiểm tra thông tin
                if (email.isEmpty() || password.isEmpty() || confirmPassword.isEmpty() ||
                        weightStr.isEmpty() || heightStr.isEmpty()) {
                    Toast.makeText(SignupActivity.this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (!password.equals(confirmPassword)) {
                    Toast.makeText(SignupActivity.this, "Mật khẩu không khớp", Toast.LENGTH_SHORT).show();
                    return;
                }

                double weight = Double.parseDouble(weightStr);
                int height = Integer.parseInt(heightStr);

                // Tính toán lượng nước cần uống
                double waterGoal = calculateWaterGoal(weight, height, healthCondition);

                auth.createUserWithEmailAndPassword(email, password)
                        .addOnCompleteListener(task -> {
                            if (task.isSuccessful() && auth.getCurrentUser() != null) {
                                String userId = auth.getCurrentUser().getUid();

                                // Tạo dữ liệu người dùng
                                Map<String, Object> user = new HashMap<>();
                                user.put("email", email);
                                user.put("weight", weight);
                                user.put("height", height);
                                user.put("healthCondition", healthCondition);
                                user.put("waterGoal", waterGoal); // Lưu mục tiêu nước

                                // Lưu vào Firestore
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
                                Toast.makeText(SignupActivity.this, "Đăng ký thất bại: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                            }
                        });
            }
        });
    }

    // Hàm tính toán lượng nước cần uống
    private double calculateWaterGoal(double weight, int height, String healthCondition) {
        // Công thức cơ bản: 33 ml/kg cân nặng (theo khuyến nghị chung)
        double baseWater = weight * 33;

        // Điều chỉnh theo chiều cao (giả định: cứ 10cm thêm 50ml)
        double heightAdjustment = (height / 10.0) * 50;

        // Điều chỉnh theo tình trạng sức khỏe
        double healthAdjustment;
        switch (healthCondition) {
            case "Người tập gym":
                healthAdjustment = baseWater * 0.2; // Tăng 20% cho người tập gym
                break;
            case "Phụ nữ mang thai":
                healthAdjustment = baseWater * 0.3; // Tăng 30% cho phụ nữ mang thai
                break;
            case "Người cao tuổi":
                healthAdjustment = -baseWater * 0.1; // Giảm 10% cho người cao tuổi
                break;
            case "Thông thường":
            default:
                healthAdjustment = 0; // Không điều chỉnh
                break;
        }

        double totalWater = baseWater + heightAdjustment + healthAdjustment;
        // Giới hạn tối thiểu 1500ml và tối đa 4000ml
        return Math.max(1500, Math.min(4000, totalWater));
    }
}