package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import androidx.annotation.NonNull;
import android.view.MenuItem;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDb;

    private TextView profileName, profileRole, profileDistrict, profileJoined;
    private TextView cardTitle1, cardTitle2, cardTitle3, cardText1, cardText2, cardText3;
    private ImageView logoutIcon, profileImage;
    private TextView editProfileButton;

    private BottomNavigationView bottomNavigationView;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure this is correct XML layout

        // Initialize bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_profile);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;

                } else if (itemId == R.id.nav_search) {
                    // Uncomment when you create SearchActivity
                     startActivity(new Intent(getApplicationContext(), SearchUserActivity.class));
                     overridePendingTransition(0, 0);
                    finish();
                    return true;

                } else if (itemId == R.id.nav_issues) {
                    startActivity(new Intent(getApplicationContext(), MyIssueActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;

                } else if (itemId == R.id.nav_profile) {
                    return true; // Already on profile
                }

                return false;
            }
        });

        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();
        realtimeDb = FirebaseDatabase.getInstance().getReference("profile");

        profileName = findViewById(R.id.profile_name);
        profileRole = findViewById(R.id.profile_role);
        profileDistrict = findViewById(R.id.profile_district);
        profileJoined = findViewById(R.id.profile_joined);

        cardTitle1 = findViewById(R.id.cardTitle1);
        cardTitle2 = findViewById(R.id.cardTitle2);
        cardTitle3 = findViewById(R.id.cardTitle3);
        cardText1 = findViewById(R.id.cardText1);
        cardText2 = findViewById(R.id.cardText2);
        cardText3 = findViewById(R.id.cardText3);

        logoutIcon = findViewById(R.id.logout_icon);
        profileImage = findViewById(R.id.profile_image); // The ImageView for profile image
        editProfileButton = findViewById(R.id.btn_edit_profile); // Assuming there's a button to edit profile

        logoutIcon.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("Logout")
                    .setMessage("Are you sure you want to log out?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        // Perform logout
                        mAuth.signOut();
                        startActivity(new Intent(this, LoginActivity.class));
                        finish();
                    })
                    .setNegativeButton("No", (dialog, which) -> {
                        dialog.dismiss(); // Just close the dialog
                    })
                    .setCancelable(true)
                    .show();
        });

        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        // Load the user profile with real-time updates
        loadUserProfile();

        // Set up Edit Profile button
        editProfileButton.setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, EditProfileActivity.class));
        });
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        // Listen for changes in the Firestore document using snapshot listener
        firestore.collection("users").document(uid)
                .addSnapshotListener((documentSnapshot, e) -> {
                    if (e != null) {
                        Toast.makeText(MainActivity.this, "Failed to load profile: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (documentSnapshot != null && documentSnapshot.exists()) {
                        String firstName = documentSnapshot.getString("firstName");
                        String lastName = documentSnapshot.getString("lastName");
                        String district = documentSnapshot.getString("district");
                        String role = documentSnapshot.getString("role");
                        String profileImageUrl = documentSnapshot.getString("profileImageUrl"); // Get image URL
                        Timestamp joinTimestamp = documentSnapshot.getTimestamp("loginDate");

                        // Update the UI with the retrieved data
                        profileName.setText(firstName + " " + lastName);
                        profileDistrict.setText("District: " + district);
                        profileRole.setText("Role: " + role);

                        if (joinTimestamp != null) {
                            Date date = joinTimestamp.toDate();
                            String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
                            profileJoined.setText("Joined: " + formattedDate);
                        }

                        // Load the profile image, either from Firestore or default
                        if (profileImageUrl != null && !profileImageUrl.isEmpty()) {
                            Glide.with(this)
                                    .load(profileImageUrl)
                                    .into(profileImage); // Load the image into the ImageView
                        } else {
                            profileImage.setImageResource(R.drawable.circle_bg); // Default image if no profile image
                        }

                        loadStatsForRole(role, uid);
                    }
                });
    }

    private void loadStatsForRole(String role, String uid) {
        String roleKey = role.equalsIgnoreCase("Government Employee") ? "gov" : "citizen";

        DatabaseReference userStatsRef = realtimeDb.child(roleKey).child(uid);
        userStatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    HashMap<String, Object> defaultStats = new HashMap<>();
                    if (roleKey.equals("citizen")) {
                        defaultStats.put("issue_submitted", 0);
                        defaultStats.put("resloved_by_gov", 0);
                        defaultStats.put("pending_issues", 0);
                    } else {
                        defaultStats.put("total_issue_taken", 0);
                        defaultStats.put("issue_in_progress", 0);
                        defaultStats.put("issue_resloved", 0);

                    }
                    userStatsRef.setValue(defaultStats);
                    updateUIStats(defaultStats, roleKey);
                } else {
                    HashMap<String, Object> stats = (HashMap<String, Object>) snapshot.getValue();
                    updateUIStats(stats, roleKey);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(MainActivity.this, "Failed to load stats", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUIStats(HashMap<String, Object> stats, String roleKey) {
        if (roleKey.equals("citizen")) {
            cardTitle1.setText("Issues Submitted");
            cardText1.setText("Total: " + stats.get("issue_submitted") + " issues reported by user.");
            cardTitle2.setText("Resolved By Gov.");
            cardText2.setText(stats.get("resloved_by_gov") + " issues of user have been successfully resolved.");
            cardTitle3.setText("Pending Issues");
            cardText3.setText("Currently " + stats.get("pending_issues") + " issues are under review or pending response.");
        } else {
            cardTitle1.setText("Issues Resolved");
            cardText1.setText("Total Issue Resolved by user: " + stats.get("issue_resloved"));
            cardTitle2.setText("Issues In Progress");
            cardText2.setText("Total Issues taken by user and In Progress: " + stats.get("issue_in_progress"));
            cardTitle3.setText("Total Issue Taken");
            cardText3.setText("Total Issue Taken To Resolve: " + stats.get("total_issue_taken"));

        }
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Do you want to exit the app?")
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity(); // 👈 Proper way to exit the app
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }


}
