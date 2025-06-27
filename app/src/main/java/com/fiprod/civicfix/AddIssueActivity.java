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
import android.preference.PreferenceManager;
import android.provider.OpenableColumns;
import android.view.View;
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
import com.google.firebase.database.*;

import org.json.JSONObject;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

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

    private TextInputEditText etTitle, etDescription;
    private TextView tvSelectArea;
    private Spinner spinnerCategory;
    private ImageView ivUpload;
    private Uri imageUri;
    private String district = "";
    private String selectedLocation = ""; // Will store "latitude,longitude"
    private MaterialButton btnSubmit;
    private FirebaseUser currentUser;
    private TextView tvUploadText;
    private LinearLayout layoutUpload;

    // Map related views
    private LinearLayout mapContainer;
    private MapView mapView;
    private MaterialButton btnConfirmLocation, btnCancelMap;
    private Marker selectedLocationMarker;
    private GeoPoint selectedPoint;
    private boolean isMapVisible = false;

    private static final int IMAGE_PICK_CODE = 1000;
    private static final int PERMISSION_CODE = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        setContentView(R.layout.activity_add_issue);

        initViews();
        currentUser = FirebaseAuth.getInstance().getCurrentUser();

        fetchUserDistrict();
        setupSpinner();
        setupImageUpload();
        setupLocationPicker();
        setupMap();
        setupSubmitAction();

        ImageView backToNav = findViewById(R.id.backToNav);
        backToNav.setOnClickListener(v -> {
            if (isMapVisible) {
                hideMap();
            } else {
                onBackPressed();
            }
        });
    }

    private void initViews() {
        etTitle = findViewById(R.id.etTitle);
        etDescription = findViewById(R.id.etDescription);
        tvSelectArea = findViewById(R.id.tvSelectArea);
        spinnerCategory = findViewById(R.id.spinnerCategory);
        layoutUpload = findViewById(R.id.layoutUploadPhoto);
        tvUploadText = layoutUpload.findViewById(R.id.uploadTextView);
        ivUpload = layoutUpload.findViewById(R.id.uploadImageView);
        btnSubmit = findViewById(R.id.btnSubmitReport);

        // Map related views
        mapContainer = findViewById(R.id.mapContainer);
        mapView = findViewById(R.id.mapView);
        btnConfirmLocation = findViewById(R.id.btnConfirmLocation);
        btnCancelMap = findViewById(R.id.btnCancelMap);
    }

    private void fetchUserDistrict() {
        if (currentUser == null) return;
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    district = snapshot.child("district").getValue(String.class);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AddIssueActivity.this, "Failed to fetch district", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        List<String> categories = Arrays.asList("Broken Road", "Garbage", "Water Problem", "Street Light", "Other");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                R.layout.spinner_selected_item,
                categories
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCategory.setAdapter(adapter);
    }

    private void setupImageUpload() {
        ivUpload.setOnClickListener(v -> {
            if (checkAndRequestPermission()) {
                openImagePicker();
            }
        });
    }

    private void setupLocationPicker() {
        tvSelectArea.setOnClickListener(v -> showMap());
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(15.0);

        // Default location (Dhaka, Bangladesh)
        GeoPoint startPoint = new GeoPoint(23.8103, 90.4125);
        mapController.setCenter(startPoint);

        // Add map click listener
        mapView.getOverlays().add(new org.osmdroid.views.overlay.Overlay() {
            @Override
            public boolean onSingleTapConfirmed(android.view.MotionEvent e, MapView mapView) {
                GeoPoint geoPoint = (GeoPoint) mapView.getProjection().fromPixels((int) e.getX(), (int) e.getY());
                addMarker(geoPoint);
                return true;
            }
        });

        // Setup map buttons
        btnConfirmLocation.setOnClickListener(v -> {
            if (selectedPoint != null) {
                selectedLocation = selectedPoint.getLatitude() + "," + selectedPoint.getLongitude();
                tvSelectArea.setText("📍 Location Selected: " + String.format("%.6f, %.6f",
                        selectedPoint.getLatitude(), selectedPoint.getLongitude()));
                tvSelectArea.setTextColor(getResources().getColor(android.R.color.holo_green_dark));
                hideMap();
                Toast.makeText(this, "Location confirmed!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Please select a location on the map first", Toast.LENGTH_SHORT).show();
            }
        });

        btnCancelMap.setOnClickListener(v -> hideMap());
    }

    private void showMap() {
        mapContainer.setVisibility(View.VISIBLE);
        isMapVisible = true;
        if (mapView != null) {
            mapView.onResume();
        }
    }

    private void hideMap() {
        mapContainer.setVisibility(View.GONE);
        isMapVisible = false;
        if (mapView != null) {
            mapView.onPause();
        }
    }

    private void addMarker(GeoPoint point) {
        // Remove existing marker if any
        if (selectedLocationMarker != null) {
            mapView.getOverlays().remove(selectedLocationMarker);
        }

        // Add new marker
        selectedLocationMarker = new Marker(mapView);
        selectedLocationMarker.setPosition(point);
        selectedLocationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        selectedLocationMarker.setTitle("Selected Location");

        mapView.getOverlays().add(selectedLocationMarker);
        mapView.invalidate();

        selectedPoint = point;

        Toast.makeText(this, "Location selected! Tap confirm to proceed.", Toast.LENGTH_SHORT).show();
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == IMAGE_PICK_CODE && resultCode == RESULT_OK && data != null) {
            imageUri = data.getData();
            Toast.makeText(this, "Image selected", Toast.LENGTH_SHORT).show();
            String imageName = getFileNameFromUri(imageUri);
            tvUploadText.setText(imageName);
        }
    }

    private void setupSubmitAction() {
        btnSubmit.setOnClickListener(v -> {
            String title = etTitle.getText().toString().trim();
            String desc = etDescription.getText().toString().trim();
            String category = spinnerCategory.getSelectedItem().toString();

            if (title.isEmpty() || desc.isEmpty() || selectedLocation.isEmpty() || imageUri == null) {
                Toast.makeText(this, "Fill all fields, select location & image", Toast.LENGTH_SHORT).show();
                return;
            }

            uploadIssueToCloudinary(title, desc, category, selectedLocation);
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
                    String uid = currentUser.getUid();
                    DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);
                    userRef.addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String fullName = "Unknown";
                            if (snapshot.exists()) {
                                String firstName = snapshot.child("firstName").getValue(String.class);
                                String lastName = snapshot.child("lastName").getValue(String.class);
                                fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                                if (fullName.isEmpty()) fullName = "Unknown";
                            }

                            String issueId = UUID.randomUUID().toString();
                            Map<String, Object> issueData = new HashMap<>();
                            issueData.put("title", title);
                            issueData.put("description", desc);
                            issueData.put("category", category);
                            issueData.put("area", area); // This will store "latitude,longitude"
                            issueData.put("district", district);
                            issueData.put("status", "PENDING");
                            issueData.put("imageUrl", imageUrl);
                            issueData.put("timestamp", new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault()).format(new Date()));

                            Map<String, Object> submittedBy = new HashMap<>();
                            submittedBy.put("userId", uid);
                            submittedBy.put("name", fullName);
                            issueData.put("submittedBy", submittedBy);

                            issueData.put("upvotes", 0);
                            issueData.put("upvotedBy", new HashMap<>());
                            issueData.put("isReported", false);

                            FirebaseDatabase.getInstance()
                                    .getReference("issues")
                                    .child(issueId)
                                    .setValue(issueData)
                                    .addOnSuccessListener(aVoid -> {
                                        dialog.dismiss();
                                        Toast.makeText(AddIssueActivity.this, "Issue submitted!", Toast.LENGTH_SHORT).show();
                                        updateProfileStatus();
                                        finish();
                                    })
                                    .addOnFailureListener(e -> {
                                        dialog.dismiss();
                                        Toast.makeText(AddIssueActivity.this, "Database error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                    });
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            dialog.dismiss();
                            Toast.makeText(AddIssueActivity.this, "Failed to fetch user info", Toast.LENGTH_SHORT).show();
                        }
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

    void updateProfileStatus() {
        DatabaseReference db = FirebaseDatabase.getInstance().getReference();
        String citizenUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        DatabaseReference citizenRef = db.child("profile").child("citizen").child(citizenUserId);

        citizenRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    long submitted = snapshot.child("issue_submitted").getValue(Long.class) != null ?
                            snapshot.child("issue_submitted").getValue(Long.class) : 0;
                    long resolved = snapshot.child("resolved_by_gov").getValue(Long.class) != null ?
                            snapshot.child("resolved_by_gov").getValue(Long.class) : 0;

                    long newSubmitted = submitted + 1;
                    long newPending = newSubmitted - resolved;

                    citizenRef.child("issue_submitted").setValue(newSubmitted);
                    citizenRef.child("pending_issues").setValue(newPending);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getApplicationContext(), "Error fetching profile data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mapView != null && isMapVisible) {
            mapView.onResume();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    @Override
    public void onBackPressed() {
        if (isMapVisible) {
            hideMap();
        } else {
            super.onBackPressed();
        }
    }
}