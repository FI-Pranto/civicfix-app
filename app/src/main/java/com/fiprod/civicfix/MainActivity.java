package com.fiprod.civicfix;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.Timestamp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainActivity extends AppCompatActivity {

    TextView emailTV, nameTV, dateTV, officeTV, designationTV, idCardTV;
    Button logoutBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }


        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                        View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
        setContentView(R.layout.activity_main);

        emailTV = findViewById(R.id.emailTextView);
        nameTV = findViewById(R.id.nameTextView);
        dateTV = findViewById(R.id.dateTextView);
        officeTV = findViewById(R.id.officeTextView);
        designationTV = findViewById(R.id.designationTextView);
        idCardTV = findViewById(R.id.idCardTextView);
        logoutBtn = findViewById(R.id.logoutButton);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        loadUserInfo();

        logoutBtn.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });
    }

    private void loadUserInfo() {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        String uid = mAuth.getCurrentUser().getUid();
        db.collection("users").document(uid).get().addOnSuccessListener(document -> {
            if (document.exists()) {
                emailTV.setText("Email: " + document.getString("email"));
                nameTV.setText("Name: " + document.getString("firstName") + " " + document.getString("lastName"));

                Timestamp ts = document.getTimestamp("loginDate");
                if (ts != null)
                    dateTV.setText("Login Date: " + ts.toDate().toString());

                String role = document.getString("role");
                if ("Government Employee".equals(role)) {
                    officeTV.setText("Office: " + document.getString("officeName"));
                    designationTV.setText("Designation: " + document.getString("designation"));
                    idCardTV.setText("ID Card Number: " + document.getString("idCardNumber"));

                    officeTV.setVisibility(TextView.VISIBLE);
                    designationTV.setVisibility(TextView.VISIBLE);
                    idCardTV.setVisibility(TextView.VISIBLE);
                }
            }
        }).addOnFailureListener(e -> {
            Toast.makeText(this, "Failed to fetch user info", Toast.LENGTH_SHORT).show();
        });
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }
}
