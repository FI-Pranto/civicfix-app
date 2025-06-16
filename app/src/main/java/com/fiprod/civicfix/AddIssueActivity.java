package com.fiprod.civicfix;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.*;

public class AddIssueActivity extends AppCompatActivity {

    private TextInputEditText etTitle, etDescription, etManualArea;
    private Spinner spinnerCategory;
    private ImageView ivUpload;
    private Uri imageUri;
    private String district = "";
    private MaterialButton btnSubmit;
    private FirebaseUser currentUser;
    private TextView tvUploadText;
    LinearLayout layoutUpload;

    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_add_issue);

        initViews();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        fetchUserDistrict();
        setupSpinner();
        setupImageUpload();
        setupSubmitAction();
        ImageView backToNav = findViewById(R.id.backToNav);
        backToNav.setOnClickListener(v -> onBackPressed());
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        etManualArea = findViewById(R.id.etManualArea);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        layoutUpload = findViewById(R.id.layoutUploadPhoto);
        tvUploadText = layoutUpload.findViewById(R.id.uploadTextView);
        ivUpload = layoutUpload.findViewById(R.id.uploadImageView);
        btnSubmit = findViewById(R.id.btnSubmitReport);
    }

    private void fetchUserDistrict() {
        if (currentUser == null) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(currentUser.getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        district = documentSnapshot.getString("district");
                    }
                });
    }

    private void setupSpinner() {
        List<String> categories = Arrays.asList("Broken Road", "Garbage", "Water Problem", "Street Light", "Other");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, categories);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupImageUpload() {
        ivUpload.setOnClickListener(v -> {
            if (checkAndRequestPermission()) {
                openImagePicker();
            }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == PERMISSION_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission denied! Cannot select image.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();

            // ✅ Show selected image name in TextView
            String imageName = getFileNameFromUri(imageUri);
            tvUploadText.setText(imageName);
        }
    }

    private void setupSubmitAction() {
        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();
            String area = etManualArea.getText().toString().trim();

            if (title.isEmpty() || desc.isEmpty() || area.isEmpty() || imageUri == null) {
                Toast.makeText(this, "Fill all fields & select image", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadIssueToCloudinary(title, desc, category, area);
        });
    }

    private void uploadIssueToCloudinary(String title, String desc, String category, String area) {
        ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage("Uploading...");
        dialog.show();

        new Thread(() -> {
            try {
                String cloudName = "ddnttmsxs";
                String uploadPreset = "CivicFix";

                InputStream inputStream = getContentResolver().openInputStream(imageUri);
                ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
                int bufferSize = 1024;
                byte[] buffer = new byte[bufferSize];
                int len;
                while ((len = inputStream.read(buffer)) != -1) {
                    byteBuffer.write(buffer, 0, len);
                }
                byte[] imageData = byteBuffer.toByteArray();

                URL url = new URL("https://api.cloudinary.com/v1_1/" + cloudName + "/image/upload");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);

                String boundary = UUID.randomUUID().toString();
                String CRLF = "\r\n";
                conn.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);

                DataOutputStream output = new DataOutputStream(conn.getOutputStream());

                output.writeBytes("--" + boundary + CRLF);
                output.writeBytes("Content-Disposition: form-data; name=\"upload_preset\"" + CRLF + CRLF);
                output.writeBytes(uploadPreset + CRLF);

                output.writeBytes("--" + boundary + CRLF);
                output.writeBytes("Content-Disposition: form-data; name=\"file\"; filename=\"image.jpg\"" + CRLF);
                output.writeBytes("Content-Type: image/jpeg" + CRLF + CRLF);
                output.write(imageData);
                output.writeBytes(CRLF);
                output.writeBytes("--" + boundary + "--" + CRLF);
                output.flush();
                output.close();

                InputStream responseStream = conn.getInputStream();
                BufferedReader in = new BufferedReader(new InputStreamReader(responseStream));
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) response.append(inputLine);
                in.close();

                JSONObject jsonResponse = new JSONObject(response.toString());
                String imageUrl = jsonResponse.getString("secure_url");

                runOnUiThread(() -> {
                    String issueId = UUID.randomUUID().toString();
                    Map<String, Object> issueData = new HashMap<>();
                    issueData.put("title", title);
                    issueData.put("description", desc);
                    issueData.put("category", category);
                    issueData.put("area", area);
                    issueData.put("district", district);
                    issueData.put("status", "PENDING");
                    issueData.put("imageUrl", imageUrl);
                    issueData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                    Map<String, Object> submittedBy = new HashMap<>();
                    submittedBy.put("userId", currentUser.getUid());
                    submittedBy.put("name", currentUser.getDisplayName());
                    issueData.put("submittedBy", submittedBy);

                    issueData.put("upvotes", 0);

                    Map<String, Object> upvotedBy = new HashMap<>();
                    issueData.put("upvotedBy", upvotedBy);

                    issueData.put("isReported", false);

                    FirebaseDatabase.getInstance()
                            .getReference("issues")
                            .child(issueId)
                            .setValue(issueData)
                            .addOnSuccessListener(aVoid -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Issue submitted!", Toast.LENGTH_SHORT).show();
                                finish();
                            })
                            .addOnFailureListener(e -> {
                                dialog.dismiss();
                                Toast.makeText(this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                });

            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> {
                    dialog.dismiss();
                    Toast.makeText(this, "Upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        }).start();
    }

    // ✅ Helper method to get filename from URI
    @SuppressLint("Range")
    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getPath();
            int cut = result.lastIndexOf('/');
            if (cut != -1) {
                result = result.substring(cut + 1);
            }
        }
        return result;
    }
}
