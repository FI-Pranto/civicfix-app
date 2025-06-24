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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_home);

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

        // Load data
        dbRef = FirebaseDatabase.getInstance().getReference("issues");
        loadIssuesFromFirebase();

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

    private void loadIssuesFromFirebase() {
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
                Toast.makeText(HomeActivity.this, "Failed to load issues", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void setupSpinner() {
        String[] statuses = {"All", "PENDING", "IN_PROGRESS", "DONE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
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
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finishAffinity(); // 👈 Proper way to exit the app
                    }
                })
                .setNegativeButton("No", null)
                .show();
    }

}
