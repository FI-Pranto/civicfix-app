package com.fiprod.civicfix;

import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.android.material.button.MaterialButton;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;

import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;
import java.util.Map;

public class IssueDetailsActivity extends AppCompatActivity {

    TextView tvTitle, tvDescription, tvCategory, tvArea, tvStatus, tvUpvotes, tvReportedBy, tvHandledBy, tvAlreadyReported;
    ImageView ivIssueImage, ivLocationIcon;

    ImageView backHome;
    Spinner spinnerStatus;
    MaterialButton btnReport;

    // Map related views
    private LinearLayout mapContainer;
    private MapView mapView;
    private MaterialButton btnCloseMap;
    private boolean isMapVisible = false;

    String issueId;
    private boolean _IsReported = false;
    private boolean _IsDone = false;
    private String locationCoordinates; // Will store "latitude,longitude"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Initialize OSMDroid configuration
        Configuration.getInstance().load(this, PreferenceManager.getDefaultSharedPreferences(this));

        //EdgeToEdge.enable(this);
        setContentView(R.layout.activity_issue_details);

        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvCategory = findViewById(R.id.tvCategory);
        tvArea = findViewById(R.id.tvArea);
        tvStatus = findViewById(R.id.tvStatus);
        tvUpvotes = findViewById(R.id.tvUpvotes);
        tvReportedBy = findViewById(R.id.tvReportedBy);
        tvHandledBy = findViewById(R.id.tvHandledBy);
        ivIssueImage = findViewById(R.id.ivIssueImage);
        ivLocationIcon = findViewById(R.id.ivLocationIcon);
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnReport = findViewById(R.id.btnReport);
        tvAlreadyReported = findViewById(R.id.tvAlreadyReported);

        // Map related views
        mapContainer = findViewById(R.id.mapContainer);
        mapView = findViewById(R.id.mapView);
        btnCloseMap = findViewById(R.id.btnCloseMap);

        backHome = findViewById(R.id.back_home);

        issueId = getIntent().getStringExtra("id");
        String title = getIntent().getStringExtra("title");
        String description = getIntent().getStringExtra("description");
        String status = getIntent().getStringExtra("status");
        String category = getIntent().getStringExtra("category");
        String area = getIntent().getStringExtra("area");
        String imageUrl = getIntent().getStringExtra("imageUrl");
        int upvotes = getIntent().getIntExtra("upvotes", 0);
        String submittedBy = getIntent().getStringExtra("submittedBy");
        String timestamp = getIntent().getStringExtra("timestamp");
        String handledBy = getIntent().getStringExtra("handledBy");

        // Store the coordinates for map display
        locationCoordinates = area;

        if (status != null) {
            switch (status.toUpperCase()) {
                case "DONE":
                    tvStatus.setBackgroundResource(R.drawable.status_resolved); // green bg
                    tvStatus.setTextColor(Color.WHITE);
                    break;
                case "IN_PROGRESS":
                    tvStatus.setBackgroundResource(R.drawable.status_in_progress); // blue bg
                    tvStatus.setTextColor(Color.WHITE);
                    break;
                case "PENDING":
                default:
                    tvStatus.setBackgroundResource(R.drawable.status_pending); // yellow bg
                    tvStatus.setTextColor(Color.WHITE);
                    break;
            }
        }

        tvTitle.setText(title);
        tvDescription.setText(description);
        tvCategory.setText(category);
        tvArea.setText(area);
        tvStatus.setText(status);
        tvUpvotes.setText(upvotes + " Upvotes");

        String formattedDate = formatDate(timestamp);
        tvReportedBy.setText("Reported by " + submittedBy + " on " + formattedDate);
        Glide.with(this).load(imageUrl).into(ivIssueImage);

        // Setup map functionality
        setupMap();
        setupLocationIcon();

        DatabaseReference issueRef = FirebaseDatabase.getInstance().getReference("issues").child(issueId);

        issueRef.child("isReported").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isReported = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(isReported)) {
                    _IsReported = true;
                    tvAlreadyReported.setVisibility(View.VISIBLE);
                    tvAlreadyReported.setText("\u26A0 This issue has been reported and is under review.");
                    btnReport.setEnabled(false);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        issueRef.child("isDone").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isDone = snapshot.getValue(Boolean.class);
                if (Boolean.TRUE.equals(isDone)) {
                    _IsDone = true;
                    spinnerStatus.setVisibility(View.GONE);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {}
        });

        if (handledBy != null && !handledBy.isEmpty()) {
            FirebaseDatabase.getInstance().getReference("users").child(handledBy)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            if (snapshot.exists()) {
                                String firstName = snapshot.child("firstName").getValue(String.class);
                                String lastName = snapshot.child("lastName").getValue(String.class);
                                String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                                tvHandledBy.setText("This issue is handled by: " + (fullName.isEmpty() ? "Unknown" : fullName));
                            } else {
                                tvHandledBy.setText("This issue is handled by: Unknown");
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            tvHandledBy.setText("This issue is handled by: Unknown");
                        }
                    });
        } else {
            tvHandledBy.setText("This issue is handled by: Not assigned");
        }

        btnReport.setOnClickListener(v -> {
            if (_IsReported) {
                Toast.makeText(this, "Already reported", Toast.LENGTH_SHORT).show();
                return;
            }
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            issueRef.child("isReported").setValue(true);
            issueRef.child("reportedBy").setValue(userId);
            Toast.makeText(this, "Reported!", Toast.LENGTH_SHORT).show();
            btnReport.setEnabled(false);
            _IsReported = true;
            tvAlreadyReported.setVisibility(View.VISIBLE);
            tvAlreadyReported.setText("\u26A0 This issue has been reported and is under review.");
        });

        String currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseDatabase.getInstance().getReference("users").child(currentUserId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (snapshot.exists()) {
                            String role = snapshot.child("role").getValue(String.class);
                            if (role != null && role.trim().equalsIgnoreCase("Government Employee") && !_IsReported && !_IsDone) {
                                setupSpinnerForStatus(status);
                            } else {
                                spinnerStatus.setVisibility(View.GONE);
                            }
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Handle error if needed
                    }
                });

        backHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isMapVisible) {
                    hideMap();
                } else {
                    finish();
                }
            }
        });
    }

    private void setupMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);

        IMapController mapController = mapView.getController();
        mapController.setZoom(16.0);

        // Setup close map button
        btnCloseMap.setOnClickListener(v -> hideMap());
    }

    private void setupLocationIcon() {
        ivLocationIcon.setOnClickListener(v -> {
            if (locationCoordinates != null && !locationCoordinates.isEmpty()) {
                showLocationOnMap();
            } else {
                Toast.makeText(this, "Location data not available", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showLocationOnMap() {
        try {
            String[] coords = locationCoordinates.split(",");
            if (coords.length == 2) {
                double latitude = Double.parseDouble(coords[0].trim());
                double longitude = Double.parseDouble(coords[1].trim());

                GeoPoint point = new GeoPoint(latitude, longitude);

                // Clear existing overlays and add marker
                mapView.getOverlays().clear();

                Marker marker = new Marker(mapView);
                marker.setPosition(point);
                marker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
                marker.setTitle("Issue Location");

                mapView.getOverlays().add(marker);

                // Center map on the location
                IMapController mapController = mapView.getController();
                mapController.setCenter(point);
                mapController.setZoom(16.0);

                mapView.invalidate();
                showMap();
            } else {
                Toast.makeText(this, "Invalid location data format", Toast.LENGTH_SHORT).show();
            }
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Error parsing location coordinates", Toast.LENGTH_SHORT).show();
        }
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

    private void setupSpinnerForStatus(String currentStatus) {
        String[] statuses = {"PENDING", "IN_PROGRESS", "DONE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_item, statuses) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.BLACK);
                ((TextView) view).setPadding(20, 20, 20, 20);
                return view;
            }

            @Override
            public View getDropDownView(int position, View convertView, android.view.ViewGroup parent) {
                View view = super.getDropDownView(position, convertView, parent);
                ((TextView) view).setTextColor(Color.BLACK);
                ((TextView) view).setPadding(20, 20, 20, 20);
                return view;
            }
        };

        spinnerStatus.setAdapter(adapter);
        spinnerStatus.setPopupBackgroundResource(android.R.color.white);
        spinnerStatus.setVisibility(View.VISIBLE);

        for (int i = 0; i < statuses.length; i++) {
            if (statuses[i].equalsIgnoreCase(currentStatus)) {
                spinnerStatus.setSelection(i);
                break;
            }
        }

        spinnerStatus.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                String newStatus = statuses[position];
                if (!newStatus.equalsIgnoreCase(tvStatus.getText().toString())) {
                    tvStatus.setText(newStatus.toUpperCase());
                    DatabaseReference issueRef = FirebaseDatabase.getInstance().getReference("issues").child(issueId);
                    String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

                    switch (newStatus.toUpperCase()) {
                        case "PENDING":
                            tvStatus.setBackgroundResource(R.drawable.status_pending);
                            issueRef.child("handledBy").removeValue();
                            tvHandledBy.setText("This issue is being handled by: Not assigned");

                            // Update gov profile using single transaction-like update
                            DatabaseReference govRefPending = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);
                            govRefPending.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    long currentInProgress = snapshot.child("issue_in_progress").getValue(Long.class) != null ?
                                            snapshot.child("issue_in_progress").getValue(Long.class) : 0;
                                    long currentResolved = snapshot.child("issue_resolved").getValue(Long.class) != null ?
                                            snapshot.child("issue_resolved").getValue(Long.class) : 0;

                                    if (currentInProgress > 0) {
                                        long newInProgress = currentInProgress - 1;
                                        long newTotal = newInProgress + currentResolved;

                                        // Use updateChildren for atomic update
                                        Map<String, Object> updates = new HashMap<>();
                                        updates.put("issue_in_progress", newInProgress);
                                        updates.put("total_issue_taken", newTotal);
                                        govRefPending.updateChildren(updates);
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                            break;

                        case "IN_PROGRESS":
                            tvStatus.setBackgroundResource(R.drawable.status_in_progress);
                            issueRef.child("handledBy").setValue(userId);

                            FirebaseDatabase.getInstance().getReference("users").child(userId)
                                    .addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                                            if (snapshot.exists()) {
                                                String firstName = snapshot.child("firstName").getValue(String.class);
                                                String lastName = snapshot.child("lastName").getValue(String.class);
                                                String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                                                tvHandledBy.setText("This issue is being handled by: " + (fullName.isEmpty() ? "Unknown" : fullName));
                                            } else {
                                                tvHandledBy.setText("This issue is being handled by: Unknown");
                                            }
                                        }

                                        @Override
                                        public void onCancelled(@NonNull DatabaseError error) {
                                            tvHandledBy.setText("This issue is being handled by: Unknown");
                                        }
                                    });

                            // Update gov profile using single transaction-like update
                            DatabaseReference govRef = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);
                            govRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    long currentInProgress = snapshot.child("issue_in_progress").getValue(Long.class) != null ?
                                            snapshot.child("issue_in_progress").getValue(Long.class) : 0;
                                    long currentResolved = snapshot.child("issue_resolved").getValue(Long.class) != null ?
                                            snapshot.child("issue_resolved").getValue(Long.class) : 0;

                                    long newInProgress = currentInProgress + 1;
                                    long newTotal = newInProgress + currentResolved;

                                    // Use updateChildren for atomic update
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("issue_in_progress", newInProgress);
                                    updates.put("total_issue_taken", newTotal);
                                    govRef.updateChildren(updates);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                            break;

                        case "DONE":
                            tvStatus.setBackgroundResource(R.drawable.status_resolved);
                            issueRef.child("isDone").setValue(true);
                            spinnerStatus.setVisibility(View.GONE);

                            // Update gov profile using single transaction-like update
                            DatabaseReference govDoneRef = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);
                            govDoneRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    long currentInProgress = snapshot.child("issue_in_progress").getValue(Long.class) != null ?
                                            snapshot.child("issue_in_progress").getValue(Long.class) : 0;
                                    long currentResolved = snapshot.child("issue_resolved").getValue(Long.class) != null ?
                                            snapshot.child("issue_resolved").getValue(Long.class) : 0;

                                    long newInProgress = Math.max(currentInProgress - 1, 0);
                                    long newResolved = currentResolved + 1;
                                    long newTotal = newInProgress + newResolved;

                                    // Use updateChildren for atomic update
                                    Map<String, Object> updates = new HashMap<>();
                                    updates.put("issue_in_progress", newInProgress);
                                    updates.put("issue_resolved", newResolved);
                                    updates.put("total_issue_taken", newTotal);
                                    govDoneRef.updateChildren(updates);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });

                            // Update citizen profile separately
                            issueRef.child("submittedBy").child("userId").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String citizenId = snapshot.getValue(String.class);
                                    if (citizenId != null) {
                                        DatabaseReference citizenRef = FirebaseDatabase.getInstance().getReference("profile/citizen").child(citizenId);

                                        citizenRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snap) {
                                                long currentResolvedByGov = snap.child("resolved_by_gov").getValue(Long.class) != null ?
                                                        snap.child("resolved_by_gov").getValue(Long.class) : 0;
                                                long currentSubmitted = snap.child("issue_submitted").getValue(Long.class) != null ?
                                                        snap.child("issue_submitted").getValue(Long.class) : 0;

                                                long newResolvedByGov = currentResolvedByGov + 1;
                                                long newPending = Math.max(currentSubmitted - newResolvedByGov, 0);

                                                // Use updateChildren for atomic update
                                                Map<String, Object> citizenUpdates = new HashMap<>();
                                                citizenUpdates.put("resolved_by_gov", newResolvedByGov);
                                                citizenUpdates.put("pending_issues", newPending);
                                                citizenRef.updateChildren(citizenUpdates);
                                            }
                                            @Override
                                            public void onCancelled(@NonNull DatabaseError error) {}
                                        });
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                            break;
                    }

                    issueRef.child("status")
                            .setValue(newStatus)
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(IssueDetailsActivity.this, "Status updated to " + newStatus, Toast.LENGTH_SHORT).show()
                            );
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });
    }

    private String formatDate(String isoDate) {
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.getDefault());
            Date date = sdf.parse(isoDate);
            SimpleDateFormat output = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
            return output.format(date);
        } catch (ParseException e) {
            return "Unknown";
        }
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