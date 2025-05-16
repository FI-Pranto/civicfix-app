package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;
    private DatabaseReference realtimeDb;

    private TextView profileName, profileRole, profileDistrict, profileJoined;
    private TextView cardTitle1,cardTitle2,cardTitle3,cardText1,cardText2,cardText3;
    private ImageView logoutIcon;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main); // Ensure this is correct XML layout

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
        cardText1=findViewById(R.id.cardText1);
        cardText2=findViewById(R.id.cardText2);
        cardText3=findViewById(R.id.cardText3);

        logoutIcon = findViewById(R.id.logout_icon);

        logoutIcon.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        loadUserProfile();
    }

    private void loadUserProfile() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        String uid = user.getUid();
        firestore.collection("users").document(uid).get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        String firstName = document.getString("firstName");
                        String lastName = document.getString("lastName");
                        String district = document.getString("district");
                        String role = document.getString("role");
                        Timestamp joinTimestamp = document.getTimestamp("loginDate");

                        profileName.setText(firstName + " " + lastName);
                        profileDistrict.setText("District: " + district);
                        profileRole.setText("Role: " + role);

                        if (joinTimestamp != null) {
                            Date date = joinTimestamp.toDate();
                            String formattedDate = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()).format(date);
                            profileJoined.setText("Joined: " + formattedDate);
                        }

                        loadStatsForRole(role, uid);
                    }
                })
                .addOnFailureListener(e -> Toast.makeText(this, "Failed to load profile", Toast.LENGTH_SHORT).show());
    }

    private void loadStatsForRole(String role, String uid) {
        String roleKey = role.equalsIgnoreCase("Government Employee") ? "gov" : "citizen";

        DatabaseReference userStatsRef = realtimeDb.child(roleKey).child(uid);
        userStatsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                if (!snapshot.exists()) {
                    // Initialize default values
                    HashMap<String, Object> defaultStats = new HashMap<>();
                    if (roleKey.equals("citizen")) {
                        defaultStats.put("issue_submitted", 0);
                        defaultStats.put("resloved_by_gov", 0);
                        defaultStats.put("pending_issues", 0);
                    } else {
                        defaultStats.put("issue_in_progress", 0);
                        defaultStats.put("issue_resloved", 0);
                        defaultStats.put("issue_reported", 0);
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
            cardTitle2.setText("Resloved By Gov.");
            cardText2.setText(stats.get("resloved_by_gov") + " issues of user have been successfully resolved.");
            cardTitle3.setText("Pending Issues");
            cardText3.setText("Currently " + stats.get("pending_issues") + " issues are under review or pending response.");

        } else {
            cardTitle1.setText("Issues Resolved");
            cardText1.setText("Total Issue Resolved by user: " + stats.get("issue_resloved"));
            cardTitle2.setText("Issues In Progress");
            cardText2.setText("Total Issues taken by user and In Progress: " + stats.get("issue_in_progress"));
            cardTitle3.setText("Issues Reported");
            cardText3.setText("Resloved issues reported by citizens: " + stats.get("issue_reported"));
        }
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
