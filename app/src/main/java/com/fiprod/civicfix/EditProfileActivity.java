package com.fiprod.civicfix;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;


import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.UUID;

public class EditProfileActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private ImageView ivProfileImage;
    private EditText etFirstName, etLastName;
    private MaterialButton btnSave;
    private TextView tvChangePhoto;
    private Uri imageUri = null;
    private String profileImageUrl = ""; // Default empty URL

    private static final int PERMISSION_CODE = 2000;
    private static final int IMAGE_PICK_CODE = 1000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ivProfileImage = findViewById(R.id.edit_profile_image);
        etFirstName = findViewById(R.id.edit_first_name);
        etLastName = findViewById(R.id.edit_last_name);
        tvChangePhoto = findViewById(R.id.change_photo_text);
        btnSave = findViewById(R.id.btn_save_profile);

        // Load user data from Firebase
        loadUserProfile();

        // Set up Change Photo click action
        tvChangePhoto.setOnClickListener(v -> {
            if (checkAndRequestPermission()) {
                openImagePicker();
            }
        });

        // Save updated profile information
        btnSave.setOnClickListener(v -> saveProfileChanges());
    }

    private void loadUserProfile() {
        String userId = mAuth.getCurrentUser().getUid();
        db.collection("users").document(userId).get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String firstName = documentSnapshot.getString("firstName");
                String lastName = documentSnapshot.getString("lastName");
                profileImageUrl = documentSnapshot.getString("profileImageUrl"); // Get image URL

                // Set values to UI
                etFirstName.setText(firstName);
                etLastName.setText(lastName);

                // Load image using Glide
                if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                    Glide.with(this)
                            .load(profileImageUrl)
                            .into(ivProfileImage); // Load the image into the ImageView
                } else {
                    ivProfileImage.setImageResource(R.drawable.circle_bg); // Default image if no profile image
                }
            }
        });
    }

    private void saveProfileChanges() {
        String newFirstName = etFirstName.getText().toString().trim();
        String newLastName = etLastName.getText().toString().trim();

        if (newFirstName.isEmpty() || newLastName.isEmpty()) {
            Toast.makeText(this, "Please fill in both fields.", Toast.LENGTH_SHORT).show();
            return;
        }

        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Updating Profile...");
        dialog.show();

        // Update Firestore
        String userId = mAuth.getCurrentUser().getUid();
        if (imageUri != null) {
            uploadProfileImageToCloudinary(imageUri, dialog, userId, newFirstName, newLastName);
        } else {
            updateProfile(userId, newFirstName, newLastName, profileImageUrl, dialog); // No image update
        }
    }

    private void uploadProfileImageToCloudinary(Uri imageUri, ProgressDialog dialog, String userId, String newFirstName, String newLastName) {
        new Thread(() -> {
            try {
                // Upload image to Cloudinary (like how it was done before)
                String cloudName = "ddnttmsxs";
                String uploadPreset = "CivicFix";

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                byte[] imageData = new byte[inputStream.available()];
                inputStream.read(imageData);

                URL url = new URL("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String boundary = UUID.randomUUID().toString();
                String CRLF = "\r\n";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                OutputStream output = conn.getOutputStream();
                output.write(("--" + boundary + CRLF).getBytes());
                output.write(("Content-Disposition: form-data; name=\"upload_preset\"" + CRLF + CRLF).getBytes());
                output.write((uploadPreset + CRLF).getBytes());

                output.write(("--" + boundary + CRLF).getBytes());
                output.write(("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"" + CRLF).getBytes());
                output.write(("Content-Type: image/jpeg" + CRLF + CRLF).getBytes());
                output.write(imageData);
                output.write(CRLF.getBytes());
                output.write(("--" + boundary + "--" + CRLF).getBytes());
                output.flush();
                output.close();

                // Get image URL from Cloudinary response
                InputStream responseStream = conn.getInputStream();
                StringBuilder response = new StringBuilder();
                int i;
                while ((i = responseStream.read()) != -1) {
                    response.append((char) i);
                }
                String imageUrl = new JSONObject(response.toString()).getString("secure_url");

                runOnUiThread(() -> {
                    profileImageUrl = imageUrl;
                    updateProfile(userId, newFirstName, newLastName, profileImageUrl, dialog); // Update Firestore with new image URL
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Image upload failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }

    private void updateProfile(String userId, String firstName, String lastName, String imageUrl, ProgressDialog dialog) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("users").document(userId)
                .update("firstName", firstName, "lastName", lastName, "profileImageUrl", imageUrl)
                .addOnSuccessListener(aVoid -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private boolean checkAndRequestPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, PERMISSION_CODE);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, PERMISSION_CODE);
                return false;
            }
        }
        return true;
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, IMAGE_PICK_CODE);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Glide.with(this)
                    .load(imageUri)
                    .into(ivProfileImage); // Show selected image with Glide
        }
    }
}
