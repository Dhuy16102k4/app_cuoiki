package com.example.water_app.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

import com.bumptech.glide.Glide;
import com.example.water_app.BaseActivity;
import com.example.water_app.R;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private TextInputEditText emailInput, weightInput, heightInput, waterGoalText;
    private AutoCompleteTextView healthConditionSpinner;
    private MaterialButton updateButton;
    private ShapeableImageView profileImage;
    private String userId;
    private Uri selectedImageUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private boolean isFormatting; // Flag to prevent recursive TextWatcher calls

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        healthConditionSpinner = findViewById(R.id.healthConditionSpinner);
        waterGoalText = findViewById(R.id.waterGoalText);
        updateButton = findViewById(R.id.updateButton);
        profileImage = findViewById(R.id.profileImage);

        // Set up health condition dropdown
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

        // Set up image picker
        imagePickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedImageUri = result.getData().getData();
                        Glide.with(this)
                                .load(selectedImageUri)
                                .placeholder(R.drawable.ic_menu_icon)
                                .error(R.drawable.ic_menu_icon)
                                .into(profileImage);
                    }
                });

        // Profile image click listener
        findViewById(R.id.editProfileImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Fetch user data
        fetchUserData();

        // Update button click listener
        updateButton.setOnClickListener(v -> updateProfile());

        // Thiết lập BottomNavigationView từ BaseActivity
        setupBottomNavigation();
    }

    @Override
    protected int getSelectedNavItemId() {
        return 0; // No item selected for EditProfileActivity
    }

    private void fetchUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        emailInput.setText(document.getString("email"));
                        weightInput.setText(String.valueOf(document.getDouble("weight")));
                        Double height = document.getDouble("height");
                        if (height != null) {
                            // Convert meters to centimeters for display
                            int heightCm = (int) (height * 100);
                            heightInput.setText(String.valueOf(heightCm));
                        }
                        healthConditionSpinner.setText(document.getString("healthCondition"), false);
                        waterGoalText.setText(String.format("%.0f", document.getDouble("waterGoal")));
                        String profileImageBase64 = document.getString("profileImage");
                        if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                            Bitmap bitmap = decodeBase64ToBitmap(profileImageBase64);
                            profileImage.setImageBitmap(bitmap);
                        }
                    } else {
                        Toast.makeText(this, "Không tìm thấy dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void updateProfile() {
        String weightStr = weightInput.getText().toString().trim();
        String heightStr = heightInput.getText().toString().trim();
        String healthCondition = healthConditionSpinner.getText().toString().trim();

        // Validate inputs
        if (weightStr.isEmpty() || heightStr.isEmpty() || healthCondition.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight, height;
        try {
            weight = Double.parseDouble(weightStr);
            // Parse height as centimeters and convert to meters
            height = Double.parseDouble(heightStr);
            if (weight < 20 || weight > 300) {
                Toast.makeText(this, "Cân nặng phải từ 20 kg đến 300 kg", Toast.LENGTH_SHORT).show();
                return;
            }
            if (height < 0.5 || height > 3.0) {
                Toast.makeText(this, "Chiều cao phải từ 0.5m đến 3m", Toast.LENGTH_SHORT).show();
                return;
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Cân nặng hoặc chiều cao không hợp lệ", Toast.LENGTH_SHORT).show();
            return;
        }

        // Calculate new water goal
        double waterGoal = calculateWaterGoal(weight, height, healthCondition);

        // Prepare updated data
        Map<String, Object> user = new HashMap<>();
        user.put("weight", weight);
        user.put("height", height);
        user.put("healthCondition", healthCondition);
        user.put("waterGoal", waterGoal);

        // Convert image to Base64 if selected
        if (selectedImageUri != null) {
            try {
                String base64Image = convertImageToBase64(selectedImageUri);
                user.put("profileImage", base64Image);
            } catch (Exception e) {
                Toast.makeText(this, "Lỗi xử lý ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        // Update Firestore
        updateFirestore(user);
    }

    private void updateFirestore(Map<String, Object> user) {
        db.collection("users").document(userId).update(user)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show();
                    waterGoalText.setText(String.format("%.0f", user.get("waterGoal")));
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Cập nhật thất bại: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private double calculateWaterGoal(double weight, double height, String healthCondition) {
        double baseWater = weight * 33;
        double heightAdjustment = height * 500; //1m uống 500ml
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

    private String convertImageToBase64(Uri imageUri) throws Exception {
        InputStream inputStream = getContentResolver().openInputStream(imageUri);
        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
        inputStream.close();

        // Compress image to reduce size
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 50, byteArrayOutputStream);
        byte[] byteArray = byteArrayOutputStream.toByteArray();

        return Base64.encodeToString(byteArray, Base64.DEFAULT);
    }

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public boolean hasUnsavedChanges() {
        return !weightInput.getText().toString().isEmpty() ||
                !heightInput.getText().toString().isEmpty() ||
                !healthConditionSpinner.getText().toString().isEmpty() ||
                selectedImageUri != null;
    }
}