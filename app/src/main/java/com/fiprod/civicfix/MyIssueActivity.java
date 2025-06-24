package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

public class MyIssueActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerMyIssues;
    private IssueAdapter issueAdapter;
    private List<Issue> userIssues = new ArrayList<>();
    private DatabaseReference dbRef;
    private Button addIssueButton;

    // Role management
    private FirebaseFirestore firestore;
    private String userRole = "";
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_issue);

        // Initialize Firebase
        firestore = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Setup RecyclerView
        recyclerMyIssues = findViewById(R.id.recycler_my_issues);
        recyclerMyIssues.setLayoutManager(new LinearLayoutManager(this));
        issueAdapter = new IssueAdapter(this, userIssues);
        recyclerMyIssues.setAdapter(issueAdapter);

        // Setup Firebase reference
        dbRef = FirebaseDatabase.getInstance().getReference("issues");

        // Initialize Add Issue Button
        addIssueButton = findViewById(R.id.button_add_issue);

        // Get user role and setup UI accordingly
        getUserRoleAndSetupUI();

        // Bottom navigation
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_issues);

        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;

            } else if (itemId == R.id.nav_search) {
                startActivity(new Intent(getApplicationContext(), SearchUserActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;

            } else if (itemId == R.id.nav_issues) {
                return true;

            } else if (itemId == R.id.nav_profile) {
                startActivity(new Intent(getApplicationContext(), MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }

            return false;
        });
    }


    private void getUserRoleAndSetupUI() {
        firestore.collection("users").document(currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            userRole = document.getString("role");
                            if (userRole == null) {
                                userRole = "Citizen"; // Default role
                            }

                            setupUIBasedOnRole();
                            loadIssuesBasedOnRole();
                        } else {
                            Toast.makeText(this, "User profile not found", Toast.LENGTH_SHORT).show();
                            userRole = "Citizen"; // Default
                            setupUIBasedOnRole();
                            loadIssuesBasedOnRole();
                        }
                    } else {
                        Toast.makeText(this, "Failed to get user role", Toast.LENGTH_SHORT).show();
                        userRole = "Citizen"; // Default
                        setupUIBasedOnRole();
                        loadIssuesBasedOnRole();
                    }
                });
    }

    // Setup UI based on user role
    private void setupUIBasedOnRole() {
        if ("Citizen".equalsIgnoreCase(userRole)) {
            // Show Add Issue button for citizens
            addIssueButton.setVisibility(View.VISIBLE);
            addIssueButton.setOnClickListener(v -> {
                Intent intent = new Intent(MyIssueActivity.this, AddIssueActivity.class);
                startActivity(intent);
            });
        } else if ("Government Employee".equalsIgnoreCase(userRole)) {
            // Hide Add Issue button for government employees
            addIssueButton.setVisibility(View.GONE);
        }
    }

    // Load issues based on user role
    private void loadIssuesBasedOnRole() {
        if ("Citizen".equalsIgnoreCase(userRole)) {
            loadMyReportedIssues(); // Issues reported by citizen
        } else if ("Government Employee".equalsIgnoreCase(userRole)) {
            loadMyHandledIssues(); // Issues handled by government employee
        }
    }

    // Load issues reported by the current user (for citizens)
    private void loadMyReportedIssues() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIssues.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Issue issue = snap.getValue(Issue.class);
                    if (issue != null) {
                        issue.id = snap.getKey();

                        // Check if issue is reported by current user
                        DataSnapshot submittedBySnap = snap.child("submittedBy");
                        if (submittedBySnap.exists() &&
                                currentUserId.equals(submittedBySnap.child("userId").getValue(String.class))) {
                            userIssues.add(issue);
                        }
                    }
                }
                updateRecyclerView();

                if (userIssues.isEmpty()) {
                    Toast.makeText(MyIssueActivity.this, "No issues reported by you", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyIssueActivity.this, "Failed to load your reported issues", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Load issues handled by the current user (for government employees)
    private void loadMyHandledIssues() {
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIssues.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Issue issue = snap.getValue(Issue.class);
                    if (issue != null) {
                        issue.id = snap.getKey();

                        // Check if issue is handled by current user
                        String handledBy = snap.child("handledBy").getValue(String.class);
                        if (currentUserId.equals(handledBy)) {
                            userIssues.add(issue);
                        }
                    }
                }
                updateRecyclerView();

                if (userIssues.isEmpty()) {
                    Toast.makeText(MyIssueActivity.this, "No issues handled by you", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyIssueActivity.this, "Failed to load your handled issues", Toast.LENGTH_SHORT).show();
            }
        });
    }

    // Update RecyclerView with new data
    private void updateRecyclerView() {
        issueAdapter = new IssueAdapter(MyIssueActivity.this, userIssues);
        recyclerMyIssues.setAdapter(issueAdapter);
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