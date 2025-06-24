package com.fiprod.civicfix;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class IssueDetailsActivity extends AppCompatActivity {

    TextView tvTitle, tvDescription, tvCategory, tvArea, tvStatus, tvUpvotes, tvReportedBy, tvHandledBy, tvAlreadyReported;
    ImageView ivIssueImage;

    ImageView backHome;
    Spinner spinnerStatus;
    MaterialButton btnReport;

    String issueId;
    private boolean _IsReported = false;
    private boolean _IsDone = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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
        spinnerStatus = findViewById(R.id.spinnerStatus);
        btnReport = findViewById(R.id.btnReport);
        tvAlreadyReported = findViewById(R.id.tvAlreadyReported);

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
            FirebaseFirestore.getInstance().collection("users")
                    .document(handledBy)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (doc.exists()) {
                            String firstName = doc.getString("firstName");
                            String lastName = doc.getString("lastName");
                            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                            tvHandledBy.setText("This issue is handled by: " + (fullName.isEmpty() ? "Unknown" : fullName));
                        } else {
                            tvHandledBy.setText("This issue is handled by: Unknown");
                        }
                    })
                    .addOnFailureListener(e ->
                            tvHandledBy.setText("This issue is handled by: Unknown")
                    );
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
        FirebaseFirestore.getInstance().collection("users")
                .document(currentUserId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String role = documentSnapshot.getString("role");
                        if (role != null && role.trim().equalsIgnoreCase("Government Employee") && !_IsReported && !_IsDone) {
                            setupSpinnerForStatus(status);
                        } else {
                            spinnerStatus.setVisibility(View.GONE);
                        }
                    }
                });


        backHome.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });

    }

    private void setupSpinnerForStatus(String currentStatus) {
        String[] statuses = {"PENDING", "IN_PROGRESS", "DONE"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, statuses) {
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

                            // Decrease gov.issue_in_progress by 1 if previously assigned
                            DatabaseReference govRefPending = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);
                            govRefPending.child("issue_in_progress").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    Long val = snapshot.getValue(Long.class);
                                    if (val != null && val > 0) {
                                        govRefPending.child("issue_in_progress").setValue(val - 1);
                                        updateGovTotalIssueTaken(govRefPending);
                                    }
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                            break;

                        case "IN_PROGRESS":
                            tvStatus.setBackgroundResource(R.drawable.status_in_progress);
                            issueRef.child("handledBy").setValue(userId);

                            FirebaseFirestore.getInstance().collection("users")
                                    .document(userId)
                                    .get()
                                    .addOnSuccessListener(doc -> {
                                        if (doc.exists()) {
                                            String firstName = doc.getString("firstName");
                                            String lastName = doc.getString("lastName");
                                            String fullName = ((firstName != null ? firstName : "") + " " + (lastName != null ? lastName : "")).trim();
                                            tvHandledBy.setText("This issue is being handled by: " + (fullName.isEmpty() ? "Unknown" : fullName));
                                        } else {
                                            tvHandledBy.setText("This issue is being handled by: Unknown");
                                        }
                                    })
                                    .addOnFailureListener(e -> tvHandledBy.setText("This issue is being handled by: Unknown"));

                            // Increase gov.issue_in_progress by 1
                            DatabaseReference govRef = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);
                            govRef.child("issue_in_progress").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    long current = snapshot.getValue(Long.class) != null ? snapshot.getValue(Long.class) : 0;
                                    govRef.child("issue_in_progress").setValue(current + 1);
                                    updateGovTotalIssueTaken(govRef);
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });
                            break;

                        case "DONE":
                            tvStatus.setBackgroundResource(R.drawable.status_resolved);
                            issueRef.child("isDone").setValue(true);
                            spinnerStatus.setVisibility(View.GONE);

                            // Update gov profile
                            DatabaseReference govDoneRef = FirebaseDatabase.getInstance().getReference("profile/gov").child(userId);

                            govDoneRef.child("issue_in_progress").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    long inProgress = snapshot.getValue(Long.class) != null ? snapshot.getValue(Long.class) : 0;
                                    if (inProgress > 0) {
                                        govDoneRef.child("issue_in_progress").setValue(inProgress - 1);
                                    }

                                    // Increment issue_resolved
                                    govDoneRef.child("issue_resolved").addListenerForSingleValueEvent(new ValueEventListener() {
                                        @Override
                                        public void onDataChange(@NonNull DataSnapshot snap) {
                                            long resolved = snap.getValue(Long.class) != null ? snap.getValue(Long.class) : 0;
                                            govDoneRef.child("issue_resolved").setValue(resolved + 1);
                                            updateGovTotalIssueTaken(govDoneRef);
                                        }
                                        @Override public void onCancelled(@NonNull DatabaseError error) {}
                                    });
                                }
                                @Override public void onCancelled(@NonNull DatabaseError error) {}
                            });

                            // Update citizen profile: resolved_by_gov++
                            issueRef.child("submittedBy").child("userId").addListenerForSingleValueEvent(new ValueEventListener() {
                                @Override
                                public void onDataChange(@NonNull DataSnapshot snapshot) {
                                    String citizenId = snapshot.getValue(String.class);
                                    if (citizenId != null) {
                                        DatabaseReference citizenRef = FirebaseDatabase.getInstance().getReference("profile/citizen").child(citizenId);
                                        citizenRef.child("resolved_by_gov").addListenerForSingleValueEvent(new ValueEventListener() {
                                            @Override
                                            public void onDataChange(@NonNull DataSnapshot snap) {
                                                long resolvedByGov = snap.getValue(Long.class) != null ? snap.getValue(Long.class) : 0;
                                                citizenRef.child("resolved_by_gov").setValue(resolvedByGov + 1);

                                                // Update pending_issues = issue_submitted - resolved_by_gov
                                                citizenRef.child("issue_submitted").addListenerForSingleValueEvent(new ValueEventListener() {
                                                    @Override
                                                    public void onDataChange(@NonNull DataSnapshot subSnap) {
                                                        long submitted = subSnap.getValue(Long.class) != null ? subSnap.getValue(Long.class) : 0;
                                                        citizenRef.child("pending_issues").setValue(submitted - (resolvedByGov + 1));
                                                    }
                                                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                                                });
                                            }

                                            @Override public void onCancelled(@NonNull DatabaseError error) {}
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

    private void updateGovTotalIssueTaken(DatabaseReference govRef) {
        govRef.child("issue_in_progress").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot inProgressSnap) {
                long inProgress = inProgressSnap.getValue(Long.class) != null ? inProgressSnap.getValue(Long.class) : 0;
                govRef.child("issue_resolved").addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot resolvedSnap) {
                        long resolved = resolvedSnap.getValue(Long.class) != null ? resolvedSnap.getValue(Long.class) : 0;
                        govRef.child("total_issue_taken").setValue(inProgress + resolved);
                    }

                    @Override public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

}
