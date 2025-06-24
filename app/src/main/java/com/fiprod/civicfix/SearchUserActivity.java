package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
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
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class SearchUserActivity extends AppCompatActivity {

    private static final String TAG = "SearchUserActivity";

    private EditText searchEditText;
    private Spinner filterSpinner;
    private RecyclerView recyclerView;
    private UserAdapter userAdapter;
    private FirebaseFirestore firestore;
    private BottomNavigationView bottomNavigationView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_search_user);

        try {
            initializeViews();
            setupBottomNavigation();
            setupRecyclerView();
            setupSpinner();
            setupSearchListener();

            // Load all users initially
            searchUsers();

        } catch (Exception e) {
            Log.e(TAG, "Error in onCreate: " + e.getMessage(), e);
            Toast.makeText(this, "Error initializing search", Toast.LENGTH_SHORT).show();
        }
    }

    private void initializeViews() {
        firestore = FirebaseFirestore.getInstance();
        searchEditText = findViewById(R.id.edit_search_users);
        filterSpinner = findViewById(R.id.user_filter);
        recyclerView = findViewById(R.id.recycler_users);
        bottomNavigationView = findViewById(R.id.bottom_navigation);
    }

    private void setupBottomNavigation() {
        bottomNavigationView.setSelectedItemId(R.id.nav_search);
        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                if (itemId == R.id.nav_home) {
                    startActivity(new Intent(getApplicationContext(), HomeActivity.class));
                    overridePendingTransition(0, 0);
                    finish();
                    return true;
                }
                else if (itemId == R.id.nav_search) {
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

    private void setupRecyclerView() {
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        userAdapter = new UserAdapter(new ArrayList<>());
        recyclerView.setAdapter(userAdapter);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> spinnerAdapter = ArrayAdapter.createFromResource(
                this,
                R.array.user_roles,
                R.layout.spinner_selected_item
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        filterSpinner.setAdapter(spinnerAdapter);

        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parentView, View selectedItemView, int position, long id) {
                searchUsers();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parentView) {
                // pass
            }
        });
    }

    private void setupSearchListener() {
        searchEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                searchUsers();
            }

            @Override
            public void afterTextChanged(android.text.Editable editable) {}
        });
    }

    private void searchUsers() {
        try {
            String searchText = searchEditText.getText().toString().trim().toLowerCase();
            String selectedRole = filterSpinner.getSelectedItem().toString();

            Log.d(TAG, "Searching with text: '" + searchText + "' and role: '" + selectedRole + "'");

            Query query = firestore.collection("users");

            // Filter by role first (if not "All")
            if (!selectedRole.equalsIgnoreCase("all")) {
                query = query.whereEqualTo("role", selectedRole);
            }

            // For text search, we'll need to filter on the client side
            // because Firestore doesn't support full-text search easily
            query.get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    QuerySnapshot querySnapshot = task.getResult();
                    List<User> userList = new ArrayList<>();

                    if (querySnapshot != null) {
                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            try {
                                String firstName = document.getString("firstName");
                                String lastName = document.getString("lastName");
                                String role = document.getString("role");
                                String profileImageUrl = document.getString("profileImageUrl");

                                // Null checks
                                if (firstName == null) firstName = "";
                                if (lastName == null) lastName = "";
                                if (role == null) role = "";
                                if (profileImageUrl == null) profileImageUrl = "";

                                // Client-side text filtering
                                if (searchText.isEmpty() ||
                                        firstName.toLowerCase().contains(searchText) ||
                                        lastName.toLowerCase().contains(searchText)) {

                                    userList.add(new User(firstName, lastName, role, profileImageUrl, document.getId()));
                                }

                            } catch (Exception e) {
                                Log.e(TAG, "Error processing document: " + document.getId(), e);
                            }
                        }
                    }

                    Log.d(TAG, "Found " + userList.size() + " users");
                    userAdapter.updateUsers(userList);

                } else {
                    Log.e(TAG, "Error fetching users", task.getException());
                    Toast.makeText(SearchUserActivity.this, "Error fetching users.", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e(TAG, "Firestore query failed", e);
                Toast.makeText(SearchUserActivity.this, "Search failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });

        } catch (Exception e) {
            Log.e(TAG, "Error in searchUsers", e);
            Toast.makeText(this, "Search error: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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