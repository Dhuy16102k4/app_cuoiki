package com.example.water_app.auth;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.ImageView;
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
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class EditProfileActivity extends BaseActivity {
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private TextInputEditText emailInput, nicknameInput, weightInput, heightInput, waterGoalText;
    private AutoCompleteTextView healthConditionSpinner;
    private MaterialButton updateButton;
    private ShapeableImageView profileImage;
    private ImageView profileFrame;
    private String userId;
    private Uri selectedImageUri;
    private Uri selectedBackgroundUri;
    private int selectedTextColor; // Store the selected text color
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> backgroundPickerLauncher;
    private boolean isFormatting;
    private Calendar currentWeekStart;

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

        // Initialize currentWeekStart
        currentWeekStart = Calendar.getInstance();
        currentWeekStart.setFirstDayOfWeek(Calendar.MONDAY);
        currentWeekStart.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        currentWeekStart.set(Calendar.HOUR_OF_DAY, 0);
        currentWeekStart.set(Calendar.MINUTE, 0);
        currentWeekStart.set(Calendar.SECOND, 0);
        currentWeekStart.set(Calendar.MILLISECOND, 0);

        // Initialize views
        emailInput = findViewById(R.id.emailInput);
        nicknameInput = findViewById(R.id.nicknameInput);
        weightInput = findViewById(R.id.weightInput);
        heightInput = findViewById(R.id.heightInput);
        healthConditionSpinner = findViewById(R.id.healthConditionSpinner);
        waterGoalText = findViewById(R.id.waterGoalText);
        updateButton = findViewById(R.id.updateButton);
        profileImage = findViewById(R.id.profileImage);
        profileFrame = findViewById(R.id.profile_frame);

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

        // Fetch user data and check ranking
        fetchUserData();
        checkUserRanking();

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

                        // Load background image
                        String backgroundImageUrl = document.getString("backgroundImageUrl");
                        if (backgroundImageUrl != null && !backgroundImageUrl.isEmpty()) {
                            applyBackground(Uri.parse(backgroundImageUrl));
                        } else {
                            applyBackground(R.drawable.background5); // Default background
                        }

                        // Load text color
                        Long textColor = document.getLong("textColor");
                        if (textColor != null) {
                            selectedTextColor = textColor.intValue();
                            applyTextColor(selectedTextColor);
                        } else {
                            selectedTextColor = 0xFFFFFFFF; // Default to white
                            applyTextColor(selectedTextColor);
                        }
                    } else {
                        String email = auth.getCurrentUser().getEmail();
                        emailInput.setText(email);
                        if (email != null) {
                            nicknameInput.setText(email.split("@")[0]);
                        }
                        applyBackground(R.drawable.background5); // Default background
                        selectedTextColor = 0xFFFFFFFF; // Default text color (white)
                        applyTextColor(selectedTextColor);
                        Toast.makeText(this, "Không tìm thấy dữ liệu", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    applyBackground(R.drawable.background5); // Default background
                    selectedTextColor = 0xFFFFFFFF; // Default text color (white)
                    applyTextColor(selectedTextColor);
                });
    }

    @SuppressWarnings({"ConstantConditions", "NotifyDataSetChanged"})
    private void checkUserRanking() {
        if (!isNetworkAvailable()) {
            Toast.makeText(this, R.string.no_network, Toast.LENGTH_SHORT).show();
            applyProfileFrame(-1);
            return;
        }

        // Use last week instead of current week for ranking
        Calendar lastWeekStart = (Calendar) currentWeekStart.clone();
        lastWeekStart.add(Calendar.WEEK_OF_YEAR, -1); // Go back one week

        long startTimestamp = lastWeekStart.getTimeInMillis();
        Calendar lastWeekEnd = (Calendar) lastWeekStart.clone();
        lastWeekEnd.add(Calendar.DAY_OF_YEAR, 6);
        lastWeekEnd.set(Calendar.HOUR_OF_DAY, 23);
        lastWeekEnd.set(Calendar.MINUTE, 59);
        lastWeekEnd.set(Calendar.SECOND, 59);
        lastWeekEnd.set(Calendar.MILLISECOND, 999);
        long endTimestamp = lastWeekEnd.getTimeInMillis();

        // Log the date range for debugging
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        Log.d("EditProfileActivity", "Fetching ranking for last week: " + sdf.format(lastWeekStart.getTime()) + " to " + sdf.format(lastWeekEnd.getTime()));

        // Fetch all users
        db.collection("users").get()
                .addOnSuccessListener(userSnapshots -> {
                    Map<String, String> userNicknames = new HashMap<>();
                    Map<String, Double> userWeeklyTotals = new HashMap<>();

                    // Initialize user data
                    for (var userDoc : userSnapshots) {
                        String userId = userDoc.getId();
                        String nickname = userDoc.getString("nickname");
                        String email = userDoc.getString("email");
                        if (nickname == null && email != null) {
                            nickname = email.split("@")[0]; // Fallback: Use email prefix as nickname
                        }
                        if (nickname != null) {
                            userNicknames.put(userId, nickname);
                            userWeeklyTotals.put(userId, 0.0);
                        } else {
                            Log.w("EditProfileActivity", "Skipping user " + userId + ": No nickname or email");
                        }
                    }

                    // Fetch water history for all users in the week
                    db.collectionGroup("waterHistory")
                            .whereGreaterThanOrEqualTo("timestamp", startTimestamp)
                            .whereLessThanOrEqualTo("timestamp", endTimestamp)
                            .get()
                            .addOnSuccessListener(historySnapshots -> {
                                // Aggregate weekly totals
                                for (var doc : historySnapshots) {
                                    DocumentReference userDocRef = doc.getReference().getParent().getParent();
                                    if (userDocRef == null) {
                                        Log.w("EditProfileActivity", "Parent document is null for waterHistory entry: " + doc.getId());
                                        continue;
                                    }
                                    String userId = userDocRef.getId();
                                    Double amount = doc.getDouble("amount");
                                    Long timestamp = doc.getLong("timestamp");
                                    if (amount != null && timestamp != null && userWeeklyTotals.containsKey(userId)) {
                                        userWeeklyTotals.put(userId, userWeeklyTotals.get(userId) + amount);
                                    }
                                }

                                // Create ranking entries
                                List<RankingEntry> rankingEntries = new ArrayList<>();
                                for (String userId : userNicknames.keySet()) {
                                    double weeklyTotal = userWeeklyTotals.getOrDefault(userId, 0.0);
                                    if (weeklyTotal > 0) { // Only add users with data
                                        rankingEntries.add(new RankingEntry(userId, userNicknames.get(userId), weeklyTotal));
                                    }
                                }

                                // Sort by weekly total (descending) and limit to top 3
                                rankingEntries.sort((a, b) -> Double.compare(b.getTotalWater(), a.getTotalWater()));
                                if (rankingEntries.size() > 3) {
                                    rankingEntries = rankingEntries.subList(0, 3);
                                }

                                // Determine the rank of the current user
                                int rank = -1;
                                for (int i = 0; i < rankingEntries.size(); i++) {
                                    if (rankingEntries.get(i).getUserId().equals(userId)) {
                                        rank = i + 1; // Rank starts at 1
                                        Log.d("EditProfileActivity", "Current user found at rank: " + rank);
                                        break;
                                    }
                                }
                                if (rank == -1) {
                                    Log.d("EditProfileActivity", "Current user not in Top 3");
                                }

                                // Apply the badge based on rank
                                applyProfileFrame(rank);
                            })
                            .addOnFailureListener(e -> {
                                Log.e("EditProfileActivity", "Error fetching water history: " + e.getMessage());
                                Toast.makeText(this, "Lỗi tải lịch sử nước: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                applyProfileFrame(-1);
                            });
                })
                .addOnFailureListener(e -> {
                    Log.e("EditProfileActivity", "Error fetching users: " + e.getMessage());
                    Toast.makeText(this, "Lỗi tải danh sách người dùng: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    applyProfileFrame(-1);
                });
    }

    // Helper class to store ranking entries
    private static class RankingEntry {
        private final String userId;
        private final String nickname;
        private final double totalWater;

        public RankingEntry(String userId, String nickname, double totalWater) {
            this.userId = userId;
            this.nickname = nickname;
            this.totalWater = totalWater;
        }

        public String getUserId() {
            return userId;
        }

        public String getNickname() {
            return nickname;
        }

        public double getTotalWater() {
            return totalWater;
        }
    }

    // Check network availability
    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    // Apply badge to profile image based on rank
    private void applyProfileFrame(int rank) {
        if (profileFrame == null) {
            Log.e("EditProfileActivity", "profileFrame is null");
            return;
        }

        Log.d("EditProfileActivity", "Applying badge for rank: " + rank);
        profileFrame.setVisibility(View.VISIBLE);
        switch (rank) {
            case 1:
                profileFrame.setImageResource(R.drawable.badge_top1);
                Log.d("EditProfileActivity", "Set badge_top1.png");
                break;
            case 2:
                profileFrame.setImageResource(R.drawable.badge_top2);
                Log.d("EditProfileActivity", "Set badge_top2.png");
                break;
            case 3:
                profileFrame.setImageResource(R.drawable.badge_top3);
                Log.d("EditProfileActivity", "Set badge_top3.png");
                break;
            default:
                profileFrame.setVisibility(View.GONE);
                Log.d("EditProfileActivity", "Hid badge (not in Top 3)");
                break;
        }
    }

    private void showBackgroundSelectionDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Chọn hình nền");
        builder.setItems(new String[]{"Mặc định (background5)", "Nhập hình nền tùy chỉnh"}, (dialog, which) -> {
            if (which == 0) {
                // Apply default background and clear custom background
                applyBackground(R.drawable.background5);
                selectedBackgroundUri = null; // Indicate no custom background
            } else {
                // Launch picker for custom background
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
                .setColorListener((ColorListener) (color, colorHex) -> {
                    selectedTextColor = color;
                    applyTextColor(color);
                })
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

        double waterGoal = calculateWaterGoal(weight, height, healthCondition);

        Map<String, Object> user = new HashMap<>();
        user.put("nickname", nickname);
        user.put("weight", weight);
        user.put("height", height);
        user.put("healthCondition", healthCondition);
        user.put("waterGoal", waterGoal);
        user.put("textColor", selectedTextColor); // Save text color

        // Handle background image: remove backgroundImageUrl if default is selected
        if (selectedBackgroundUri == null) {
            user.put("backgroundImageUrl", FieldValue.delete()); // Remove custom background
        }

        // Handle profile image and background image uploads
        if (selectedImageUri != null && selectedBackgroundUri != null) {
            // Upload both profile and background images
            uploadProfileAndBackgroundImages(selectedImageUri, selectedBackgroundUri, user);
        } else if (selectedImageUri != null) {
            // Upload only profile image
            uploadImageToStorage(selectedImageUri, user);
        } else if (selectedBackgroundUri != null) {
            // Upload only background image
            uploadBackgroundImageToStorage(selectedBackgroundUri, user);
        } else {
            // No images to upload, update Firestore directly
            updateFirestore(user);
        }

        // Update ranking
        checkUserRanking();
    }

    private void uploadProfileAndBackgroundImages(Uri profileImageUri, Uri backgroundImageUri, Map<String, Object> user) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference storageRef = storage.getReference();
        StorageReference profileImageRef = storageRef.child("profile_images/" + userId + "/profile.jpg");
        StorageReference backgroundImageRef = storageRef.child("background_images/" + userId + "/background.jpg");

        // Upload profile image
        profileImageRef.putFile(profileImageUri).addOnSuccessListener(profileTaskSnapshot -> {
            profileImageRef.getDownloadUrl().addOnSuccessListener(profileUri -> {
                String profileDownloadUrl = profileUri.toString();
                user.put("profileImageUrl", profileDownloadUrl);
                user.put("profileImage", null);

                // Upload background image
                backgroundImageRef.putFile(backgroundImageUri).addOnSuccessListener(backgroundTaskSnapshot -> {
                    backgroundImageRef.getDownloadUrl().addOnSuccessListener(backgroundUri -> {
                        String backgroundDownloadUrl = backgroundUri.toString();
                        user.put("backgroundImageUrl", backgroundDownloadUrl);
                        updateFirestore(user);
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Lỗi lấy URL hình nền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
                }).addOnFailureListener(e -> {
                    Toast.makeText(this, "Lỗi tải hình nền lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi lấy URL ảnh hồ sơ: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải ảnh hồ sơ lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        });
    }

    private void uploadImageToStorage(Uri imageUri, Map<String, Object> user) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }
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

    private void uploadBackgroundImageToStorage(Uri backgroundImageUri, Map<String, Object> user) {
        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.", Toast.LENGTH_SHORT).show();
            return;
        }
        StorageReference storageRef = storage.getReference();
        StorageReference backgroundImageRef = storageRef.child("background_images/" + userId + "/background.jpg");

        backgroundImageRef.putFile(backgroundImageUri).addOnSuccessListener(taskSnapshot -> {
            backgroundImageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                String downloadUrl = uri.toString();
                user.put("backgroundImageUrl", downloadUrl);
                updateFirestore(user);
            }).addOnFailureListener(e -> {
                Toast.makeText(this, "Lỗi lấy URL hình nền: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Lỗi tải hình nền lên: " + e.getMessage(), Toast.LENGTH_SHORT).show();
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
                selectedBackgroundUri != null ||
                selectedTextColor != 0xFFFFFFFF; // Check if text color has changed
    }
}
