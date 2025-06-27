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

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.*;

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

    private String userDistrict = "";
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        mAuth = FirebaseAuth.getInstance();

        recyclerIssues = findViewById(R.id.recycler_issues);
        editSearch = findViewById(R.id.edit_search);
        spinnerFilter = findViewById(R.id.spinner_filter);
        bottomNavigationView = findViewById(R.id.bottom_navigation);

        recyclerIssues.setLayoutManager(new LinearLayoutManager(this));
        issueAdapter = new IssueAdapter(this, issueList);
        recyclerIssues.setAdapter(issueAdapter);

        setupSpinner();
        setupSearchAndFilter();
        getUserDistrictFromRealtimeDB(); // 🔄 Replaced Firestore with Realtime DB

        bottomNavigationView.setSelectedItemId(R.id.nav_home);
        bottomNavigationView.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.nav_home) {
                return true;
            } else if (itemId == R.id.nav_search) {
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
        });
    }

    private void getUserDistrictFromRealtimeDB() {
        if (mAuth.getCurrentUser() == null) {
            Toast.makeText(this, "User not authenticated", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        DatabaseReference userRef = FirebaseDatabase.getInstance().getReference("users").child(uid);

        userRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userDistrict = snapshot.child("district").getValue(String.class);
                if (userDistrict == null) userDistrict = "";
                loadDistrictIssuesFromFirebase();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(HomeActivity.this, "Failed to fetch user district", Toast.LENGTH_SHORT).show();
                loadAllIssuesFromFirebase();
            }
        });
    }

    private void loadDistrictIssuesFromFirebase() {
        if (userDistrict.isEmpty()) {
            Toast.makeText(this, "User district not found, showing all issues", Toast.LENGTH_SHORT).show();
            loadAllIssuesFromFirebase();
            return;
        }

        dbRef = FirebaseDatabase.getInstance().getReference("issues");
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
                .setPositiveButton("Yes", (dialog, which) -> finishAffinity())
                .show();
    }
}
