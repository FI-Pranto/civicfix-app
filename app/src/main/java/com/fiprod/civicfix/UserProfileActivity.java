package com.fiprod.civicfix;

import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class UserProfileActivity extends AppCompatActivity {

    private static final String TAG = "UserProfileActivity";

    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDb;

    private TextView profileName, profileRole, profileDistrict, profileJoined;
    private TextView cardTitle1, cardTitle2, cardTitle3, cardText1, cardText2, cardText3;
    private ImageView profileImage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_user_profile);

        try {
            firestore = FirebaseFirestore.getInstance();
            realtimeDb = FirebaseDatabase.getInstance().getReference("profile");

            // Initializing Views
            profileName = findViewById(R.id.profile_name);
            profileRole = findViewById(R.id.profile_role);
            profileDistrict = findViewById(R.id.profile_district);
            profileJoined = findViewById(R.id.profile_joined);
            profileImage = findViewById(R.id.profile_image);

            // Cards to display issue data
            cardTitle1 = findViewById(R.id.cardTitle1);
            cardTitle2 = findViewById(R.id.cardTitle2);
            cardTitle3 = findViewById(R.id.cardTitle3);
            cardText1 = findViewById(R.id.cardText1);
            cardText2 = findViewById(R.id.cardText2);
            cardText3 = findViewById(R.id.cardText3);

            // Get the user ID passed from the SearchUserActivity
            String userId = getIntent().getStringExtra("USER_ID");

            if (userId != null && !userId.trim().isEmpty()) {
                Log.d(TAG, "Loading user profile for ID: " + userId);
                loadUserProfile(userId);
            } else {
                Log.e(TAG, "User ID is missing or empty");
                Toast.makeText(UserProfileActivity.this, "User ID is missing", Toast.LENGTH_SHORT).show();
                finish();
            }

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing profile", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    private void loadUserProfile(String userId) {
        firestore.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    try {
                        if (documentSnapshot.exists()) {
                            Log.d(TAG, "User document found");

                            // Fetching user data with null checks
                            String firstName = documentSnapshot.getString("firstName");
                            String lastName = documentSnapshot.getString("lastName");
                            String role = documentSnapshot.getString("role");
                            String district = documentSnapshot.getString("district");
                            String profileImageUrl = documentSnapshot.getString("profileImageUrl");

                            // Handle loginDate as Timestamp (this was causing the crash)
                            String joinDateFormatted = handleLoginDate(documentSnapshot);

                            // Set data into the views with null checks
                            String fullName = ((firstName != null ? firstName : "") + " " +
                                    (lastName != null ? lastName : "")).trim();
                            if (fullName.isEmpty()) {
                                fullName = "Unknown User";
                            }
                            profileName.setText(fullName);

                            profileRole.setText("Role: " + (role != null ? role : "Not specified"));
                            profileDistrict.setText("District: " + (district != null ? district : "Not specified"));
                            profileJoined.setText("Joined: " + joinDateFormatted);

                            // Set the profile image using Glide
                            loadProfileImage(profileImageUrl);

                            // Load user stats after loading the profile
                            loadUserStats(userId, role != null ? role : "");

                        } else {
                            Log.w(TAG, "User document does not exist");
                            Toast.makeText(UserProfileActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                            finish();
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing user document: " + e.getMessage(), e);
                        Toast.makeText(UserProfileActivity.this, "Error loading profile data", Toast.LENGTH_SHORT).show();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Failed to load user profile: " + e.getMessage(), e);
                    Toast.makeText(UserProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private String handleLoginDate(DocumentSnapshot documentSnapshot) {
        String joinDateFormatted = "N/A";

        try {
            // First try to get as Timestamp (this is what's causing the original crash)
            Timestamp loginTimestamp = documentSnapshot.getTimestamp("loginDate");
            if (loginTimestamp != null) {
                Date loginDate = loginTimestamp.toDate();
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                joinDateFormatted = sdf.format(loginDate);
                Log.d(TAG, "Successfully parsed loginDate as Timestamp");
            }
        } catch (Exception e1) {
            Log.w(TAG, "Failed to read loginDate as Timestamp, trying as String: " + e1.getMessage());

            // Fallback: try to get as String
            try {
                String joinDate = documentSnapshot.getString("loginDate");
                if (joinDate != null && !joinDate.isEmpty()) {
                    // Try to parse different date formats
                    joinDateFormatted = parseStringDate(joinDate);
                }
            } catch (Exception e2) {
                Log.e(TAG, "Failed to read loginDate as String: " + e2.getMessage());
                joinDateFormatted = "Date unavailable";
            }
        }

        return joinDateFormatted;
    }

    private String parseStringDate(String dateString) {
        // Try different date format patterns
        String[] patterns = {
                "yyyy-MM-dd",
                "dd/MM/yyyy",
                "MM/dd/yyyy",
                "dd-MM-yyyy",
                "yyyy/MM/dd"
        };

        for (String pattern : patterns) {
            try {
                SimpleDateFormat inputFormat = new SimpleDateFormat(pattern, Locale.getDefault());
                Date date = inputFormat.parse(dateString);
                if (date != null) {
                    SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                    return outputFormat.format(date);
                }
            } catch (Exception e) {
                // Continue to next pattern
            }
        }

        // If no pattern works, return the original string
        return dateString;
    }

    private void loadProfileImage(String profileImageUrl) {
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            Glide.with(this)
                    .load(profileImageUrl)
                    .placeholder(R.drawable.circle_bg)
                    .error(R.drawable.circle_bg)
                    .into(profileImage);
        } else {
            profileImage.setImageResource(R.drawable.circle_bg);
        }
    }

    private void loadUserStats(String userId, String role) {
        try {
            // Reference the user's statistics in the Firebase Realtime Database
            DatabaseReference userStatsRef;
            if ("Citizen".equalsIgnoreCase(role)) {
                userStatsRef = realtimeDb.child("citizen").child(userId);
            } else {
                userStatsRef = realtimeDb.child("gov").child(userId);
            }

            userStatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot snapshot) {
                    try {
                        if (snapshot.exists()) {
                            Log.d(TAG, "User stats found in database");

                            // Fetching user statistics with null checks
                            Integer issueSubmitted = snapshot.child("issue_submitted").getValue(Integer.class);
                            Integer resolvedByGov = snapshot.child("resolved_by_gov").getValue(Integer.class);
                            Integer pendingIssues = snapshot.child("pending_issues").getValue(Integer.class);
                            Integer issueInProgress = snapshot.child("issue_in_progress").getValue(Integer.class);
                            Integer issueResolved = snapshot.child("issue_resolved").getValue(Integer.class);
                            Integer issueTaken = snapshot.child("total_issue_taken").getValue(Integer.class);

                            // Update UI with stats based on the role
                            if ("Citizen".equalsIgnoreCase(role)) {
                                updateCitizenStats(issueSubmitted, resolvedByGov, pendingIssues);
                            } else {
                                updateGovernmentStats(issueResolved, issueInProgress, issueTaken);
                            }
                        } else {
                            Log.d(TAG, "No user stats found, showing default values");
                            showDefaultStats(role);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error processing user stats: " + e.getMessage(), e);
                        showDefaultStats(role);
                    }
                }

                @Override
                public void onCancelled(DatabaseError error) {
                    Log.e(TAG, "Failed to load user stats: " + error.getMessage());
                    Toast.makeText(UserProfileActivity.this, "Failed to load statistics", Toast.LENGTH_SHORT).show();
                    showDefaultStats(role);
                }
            });
        } catch (Exception e) {
            Log.e(TAG, "Error setting up stats listener: " + e.getMessage(), e);
            showDefaultStats(role);
        }
    }

    private void updateCitizenStats(Integer issueSubmitted, Integer resolvedByGov, Integer pendingIssues) {
        cardTitle1.setText("Issues Submitted");
        cardText1.setText("Total: " + (issueSubmitted != null ? issueSubmitted : 0) + " issues reported by user.");

        cardTitle2.setText("Resolved By Gov.");
        cardText2.setText((resolvedByGov != null ? resolvedByGov : 0) + " issues have been successfully resolved by government.");

        cardTitle3.setText("Pending Issues");
        cardText3.setText("Currently " + (pendingIssues != null ? pendingIssues : 0) + " issues are under review or pending response.");
    }

    private void updateGovernmentStats(Integer issueResolved, Integer issueInProgress, Integer issueTaken) {
        cardTitle1.setText("Issues Resolved");
        cardText1.setText("Total: " + (issueResolved != null ? issueResolved : 0) + " issues resolved by government.");

        cardTitle2.setText("Issues In Progress");
        cardText2.setText("Currently " + (issueInProgress != null ? issueInProgress : 0) + " issues are in progress.");

        cardTitle3.setText("Total Issue Taken");
        cardText3.setText("Total Issue Taken To Resolve: " + (issueTaken != null ? issueTaken : 0));
    }

    private void showDefaultStats(String role) {
        if ("Citizen".equalsIgnoreCase(role)) {
            cardTitle1.setText("Issues Submitted");
            cardText1.setText("Total: 0 issues reported by user.");

            cardTitle2.setText("Resolved By Gov.");
            cardText2.setText("0 issues resolved by government.");

            cardTitle3.setText("Pending Issues");
            cardText3.setText("No pending issues.");
        } else {
            cardTitle1.setText("Issues Resolved");
            cardText1.setText("Total: 0 issues resolved by government.");

            cardTitle2.setText("Issues In Progress");
            cardText2.setText("Currently 0 issues are in progress.");

            cardTitle3.setText("Issues Reported");
            cardText3.setText("Total: 0 issues reported by citizens.");
        }
    }
}