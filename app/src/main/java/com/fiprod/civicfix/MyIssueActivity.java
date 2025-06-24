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

import java.util.ArrayList;
import java.util.List;

public class MyIssueActivity extends AppCompatActivity {

    private BottomNavigationView bottomNavigationView;
    private RecyclerView recyclerMyIssues;
    private IssueAdapter issueAdapter;
    private List<Issue> userIssues = new ArrayList<>();
    private DatabaseReference dbRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       // EdgeToEdge.enable(this);
        setContentView(R.layout.activity_my_issue);

        // Setup RecyclerView
        recyclerMyIssues = findViewById(R.id.recycler_my_issues);
        recyclerMyIssues.setLayoutManager(new LinearLayoutManager(this));
        issueAdapter = new IssueAdapter(this, userIssues);
        recyclerMyIssues.setAdapter(issueAdapter);

        // Setup Firebase reference
        dbRef = FirebaseDatabase.getInstance().getReference("issues");

        loadMyIssues();

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

        // Add Issue Button
        Button addIssueButton = findViewById(R.id.button_add_issue);
        addIssueButton.setOnClickListener(v -> {
            Intent intent = new Intent(MyIssueActivity.this, AddIssueActivity.class);
            startActivity(intent);
        });
    }

    private void loadMyIssues() {
        String currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        dbRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                userIssues.clear();
                for (DataSnapshot snap : snapshot.getChildren()) {
                    Issue issue = snap.getValue(Issue.class);
                    if (issue != null) {
                        issue.id = snap.getKey();
                        DataSnapshot submittedBySnap = snap.child("submittedBy");
                        if (submittedBySnap.exists() && currentUid.equals(submittedBySnap.child("userId").getValue(String.class))) {
                            userIssues.add(issue);
                        }
                    }
                }
                issueAdapter = new IssueAdapter(MyIssueActivity.this, userIssues);
                recyclerMyIssues.setAdapter(issueAdapter);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MyIssueActivity.this, "Failed to load your issues.", Toast.LENGTH_SHORT).show();
            }
        });
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
