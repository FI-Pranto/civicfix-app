package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
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

public class HomeActivity extends AppCompatActivity {

    private RecyclerView recyclerIssues;
    private IssueAdapter issueAdapter;
    private List<Issue> issueList = new ArrayList<>();
    private DatabaseReference dbRef;
    private EditText editSearch;
    private Spinner spinnerFilter;
    private BottomNavigationView bottomNavigationView;

    // Add these variables for district filtering
    private String userDistrict = "";
    private FirebaseAuth mAuth;
    private FirebaseFirestore firestore;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

        // Initialize Firebase Auth and Firestore
        mAuth = FirebaseAuth.getInstance();
        firestore = FirebaseFirestore.getInstance();

        // Initialize views
        recyclerIssues = findViewById(R.id.recycler_issues);
        editSearch = findViewById(R.id.edit_search);
        spinnerFilter = findViewById(R.id.spinner_filter);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        // Setup RecyclerView
        recyclerIssues.setLayoutManager(new LinearLayoutManager(this));
        issueAdapter = new IssueAdapter(this, issueList);
        recyclerIssues.setAdapter(issueAdapter);

        // Setup Spinner and Search
        setupSpinner();
        setupSearchAndFilter();

        // Get user district and load data
        getUserDistrictFromFirestore();

        // Setup bottom navigation (YOUR original logic)
        bottomNavigationView = findViewById(R.id.bottom_navigation);
        bottomNavigationView.setSelectedItemId(R.id.nav_home);

        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    return true; // Already on home

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
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }

                return false;
            }
        });
    }

    // Get user district from Firestore
    private void getUserDistrictFromFirestore() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = mAuth.getCurrentUser().getUid();

        firestore.collection("users").document(userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            userDistrict = document.getString("district");
                            if (userDistrict == null) {
                                userDistrict = "";
                            }

                            // Now load issues based on user's district
                            loadDistrictIssuesFromFirebase();
                        } else {
                            Toast.makeText(HomeActivity.this, "User profile not found", Toast.LENGTH_SHORT).show();
                            loadAllIssuesFromFirebase(); // Fallback
                        }
                    } else {
                        Toast.makeText(HomeActivity.this, "Failed to get user data: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        loadAllIssuesFromFirebase(); // Fallback
                    }
                });
    }

    // Load issues filtered by user's district
    private void loadDistrictIssuesFromFirebase() {
        if (userDistrict.isEmpty()) {
            Toast.makeText(this, "User district not found, showing all issues", Toast.LENGTH_SHORT).show();
            loadAllIssuesFromFirebase();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("issues");

        // Query issues by district
        dbRef.orderByChild("district").equalTo(userDistrict)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        issueList.clear();
                        for (DataSnapshot snap : snapshot.getChildren()) {
                            Issue issue = snap.getValue(Issue.class);
                            if (issue != null) {
                                issue.id = snap.getKey();
                                issueList.add(issue);
                            }
                        }
                        filterAndSearch();

                        // Show message if no issues found
                        if (issueList.isEmpty()) {
                            Toast.makeText(HomeActivity.this,
                                    "No issues found in " + userDistrict + " district",
                                    Toast.LENGTH_SHORT).show();
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Toast.makeText(HomeActivity.this, "Failed to load issues: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // Fallback method to load all issues
    private void loadAllIssuesFromFirebase() {
        dbRef = FirebaseDatabase.getInstance().getReference("issues");
        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                issueList.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Issue issue = snap.getValue(Issue.class);
                    if (issue != null) {
                        issue.id = snap.getKey();
                        issueList.add(issue);
                    }
                }
                filterAndSearch();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to load issues: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        String[] statuses = {"All", "PENDING", "IN_PROGRESS", "DONE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.spinner_selected_item, statuses);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(adapter);
    }

    private void setupSearchAndFilter() {
        editSearch.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterAndSearch();
            }
            public void afterTextChanged(Editable s) {}
        });

        spinnerFilter.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                filterAndSearch();
            }
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void filterAndSearch() {
        String searchText = editSearch.getText().toString().toLowerCase();
        String selectedStatus = spinnerFilter.getSelectedItem().toString();

        List<Issue> filteredList = new ArrayList<>();
        for (Issue issue : issueList) {
            boolean matchesStatus = selectedStatus.equals("All") || issue.status.equalsIgnoreCase(selectedStatus);
            boolean matchesSearch = issue.title != null && issue.title.toLowerCase().contains(searchText);

            if (matchesStatus && matchesSearch) {
                filteredList.add(issue);
            }
        }

        issueAdapter = new IssueAdapter(this, filteredList);
        recyclerIssues.setAdapter(issueAdapter);
    }

    @SuppressLint("MissingSuperCall")
    @Override
    public void onBackPressed() {
        new AlertDialog.Builder(this)
                .setTitle("Exit App")
                .setMessage("Do you want to exit the app?")
                .setNegativeButton("No", null)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity(); // 👈 Proper way to exit the app
                    }
                })
                .show();
    }
}