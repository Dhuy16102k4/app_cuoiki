package com.example.water_app.auth;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.example.water_app.BaseActivity;
import com.example.water_app.R;
import com.github.dhaval2404.colorpicker.ColorPickerDialog;
import com.github.dhaval2404.colorpicker.listener.ColorListener;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.imageview.ShapeableImageView;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import android.content.DialogInterface;


import java.util.HashMap;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private TextInputEditText emailInput, nicknameInput, weightInput, heightInput, waterGoalText;
    private AutoCompleteTextView healthConditionSpinner;
    private MaterialButton updateButton;
    private ShapeableImageView profileImage;
    private String userId;
    private Uri selectedImageUri;
    private Uri selectedBackgroundUri;
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> backgroundPickerLauncher;
    private boolean isFormatting;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        // Initialize Firebase
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        userId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;

        if (userId == null) {
            Toast.makeText(this, "Vui lòng đăng nhập", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        nicknameInput = findViewById(R.id.nicknameInput);
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
                if (isFormatting) return;

                isFormatting = true;
                String input = s.toString().trim();
                if (!input.isEmpty()) {
                    try {
                        String cleanInput = input.replace(".", "");
                        int rawHeight = Integer.parseInt(cleanInput);
                        if (rawHeight >= 50 && rawHeight <= 300) {
                            double formattedHeight = rawHeight / 100.0;
                            String formatted = String.format("%.2f", formattedHeight);
                            if (!input.equals(formatted)) {
                                heightInput.setText(formatted);
                                heightInput.setSelection(formatted.length());
                            }
                        }
                    } catch (NumberFormatException e) {
                        // Ignore invalid input
                    }
                }
                isFormatting = false;
            }
        });

        // Set up image picker for profile image
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

        // Set up image picker for background
        backgroundPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        selectedBackgroundUri = result.getData().getData();
                        applyBackground(selectedBackgroundUri);
                    }
                });

        // Profile image click listener
        findViewById(R.id.editProfileImage).setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
            imagePickerLauncher.launch(intent);
        });

        // Background selection button listener
        findViewById(R.id.select_background_button).setOnClickListener(v -> showBackgroundSelectionDialog());

        // Text color change button listener
        findViewById(R.id.change_text_color_button).setOnClickListener(v -> showColorPickerDialog());

        // Fetch user data
        fetchUserData();

        // Update button click listener
        updateButton.setOnClickListener(v -> updateProfile());

        // Set up BottomNavigationView
        setupBottomNavigation();
    }

    @Override
    protected int getSelectedNavItemId() {
        return 0; // No item selected
    }

    private void fetchUserData() {
        db.collection("users").document(userId).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String email = document.getString("email");
                        emailInput.setText(email);

                        String nickname = document.getString("nickname");
                        if (nickname == null && email != null) {
                            nickname = email.split("@")[0];
                        }
                        nicknameInput.setText(nickname);

                        weightInput.setText(String.valueOf(document.getDouble("weight")));
                        Double height = document.getDouble("height");
                        if (height != null) {
                            int heightCm = (int) (height * 100);
                            heightInput.setText(String.valueOf(heightCm));
                        }
                        healthConditionSpinner.setText(document.getString("healthCondition"), false);
                        waterGoalText.setText(String.format("%.0f", document.getDouble("waterGoal")));

                        // Load profile image
                        String profileImageUrl = document.getString("profileImageUrl");
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .placeholder(R.drawable.ic_menu_icon)
                                    .error(R.drawable.ic_menu_icon)
                                    .into(profileImage);
                        } else {
                            String profileImageBase64 = document.getString("profileImage");
                            if (profileImageBase64 != null && !profileImageBase64.isEmpty()) {
                                Bitmap bitmap = decodeBase64ToBitmap(profileImageBase64);
                                profileImage.setImageBitmap(bitmap);
                            }
                        }
                    } else {
                        String email = auth.getCurrentUser().getEmail();
                        emailInput.setText(email);
                        if (email != null) {
                            nicknameInput.setText(email.split("@")[0]);
                        }
                        Toast.makeText(this, "Không tìm thấy dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void showBackgroundSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn hình nền");
        builder.setItems(new String[]{"Mặc định (background5)", "Nhập hình nền tùy chỉnh"}, (dialog, which) -> {
            if (which == 0) {
                // Apply default background
                applyBackground(R.drawable.background5);
                selectedBackgroundUri = null;
            } else {
                // Launch image picker for custom background
                Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                backgroundPickerLauncher.launch(intent);
            }
        });
        builder.setNegativeButton("Hủy", null);
        builder.show();
    }

    private void applyBackground(Uri backgroundUri) {
        ConstraintLayout layout = findViewById(R.id.constraintLayout);
        Glide.with(this)
                .load(backgroundUri)
                .placeholder(R.drawable.background5)
                .error(R.drawable.background5)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        layout.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        layout.setBackgroundResource(R.drawable.background5);
                    }
                });
    }

    private void applyBackground(int backgroundResId) {
        ConstraintLayout layout = findViewById(R.id.constraintLayout);
        Glide.with(this)
                .load(backgroundResId)
                .placeholder(R.drawable.background5)
                .error(R.drawable.background5)
                .into(new CustomTarget<Drawable>() {
                    @Override
                    public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
                        layout.setBackground(resource);
                    }

                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {
                        layout.setBackgroundResource(R.drawable.background5);
                    }
                });
    }

    private void showColorPickerDialog() {
        new ColorPickerDialog.Builder(this)
                .setTitle("Chọn màu chữ")
                .setColorListener((ColorListener) (color, colorHex) -> applyTextColor(color))
                .setPositiveButton(R.string.ok)
                .setNegativeButton(R.string.cancel)
                .build()
                .show();
    }

    private void applyTextColor(int color) {
        TextView profileTitle = findViewById(R.id.profileTitle);
        profileTitle.setTextColor(color);

        emailInput.setTextColor(color);
        nicknameInput.setTextColor(color);
        weightInput.setTextColor(color);
        heightInput.setTextColor(color);
        waterGoalText.setTextColor(color);
        healthConditionSpinner.setTextColor(color);

        updateButton.setTextColor(color);
        MaterialButton selectBackgroundButton = findViewById(R.id.select_background_button);
        selectBackgroundButton.setTextColor(color);
        MaterialButton changeTextColorButton = findViewById(R.id.change_text_color_button);
        changeTextColorButton.setTextColor(color);
    }

    private void updateProfile() {
        String nickname = nicknameInput.getText().toString().trim();
        String weightStr = weightInput.getText().toString().trim();
        String heightStr = heightInput.getText().toString().trim();
        String healthCondition = healthConditionSpinner.getText().toString().trim();

        // Validate inputs
        if (nickname.isEmpty() || weightStr.isEmpty() || heightStr.isEmpty() || healthCondition.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show();
            return;
        }

        double weight, height;
        try {
            weight = Double.parseDouble(weightStr);
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
        user.put("nickname", nickname);
        user.put("weight", weight);
        user.put("height", height);
        user.put("healthCondition", healthCondition);
        user.put("waterGoal", waterGoal);

        // Handle profile image upload
        if (selectedImageUri != null) {
            uploadImageToStorage(selectedImageUri, user);
        } else {
            updateFirestore(user);
        }
    }

    private void uploadImageToStorage(Uri imageUri, Map<String, Object> user) {
        StorageReference storageRef = storage.getReference();
        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + "/profile.jpg");

        profileImageRef.putFile(imageUri).addOnSuccessListener(taskSnapshot -> {
            profileImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                user.put("profileImageUrl", downloadUrl);
                user.put("profileImage", null);
                updateFirestore(user);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi lấy URL ảnh: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải ảnh lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
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
        double heightAdjustment = height * 500;
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

    private Bitmap decodeBase64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    public boolean hasUnsavedChanges() {
        return !nicknameInput.getText().toString().isEmpty() ||
                !weightInput.getText().toString().isEmpty() ||
                !heightInput.getText().toString().isEmpty() ||
                !healthConditionSpinner.getText().toString().isEmpty() ||
                selectedImageUri != null ||
                selectedBackgroundUri != null;
    }
}