package com.fiprod.civicfix;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText emailET, passwordET, firstNameET, lastNameET;
    Button registerBtn;
    FirebaseAuth mAuth;
    FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        firstNameET = findViewById(R.id.firstName);
        lastNameET = findViewById(R.id.lastName);
        registerBtn = findViewById(R.id.registerButton);

        registerBtn.setOnClickListener(v -> registerUser());
    }

    private void registerUser() {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();
        String firstName = firstNameET.getText().toString().trim();
        String lastName = lastNameET.getText().toString().trim();

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                String uid = mAuth.getCurrentUser().getUid();
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("email", email);
                userMap.put("firstName", firstName);
                userMap.put("lastName", lastName);
                userMap.put("loginDate", FieldValue.serverTimestamp());

                db.collection("users").document(uid).set(userMap).addOnSuccessListener(unused -> {
                    Toast.makeText(this, "Registered!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                });
            } else {
                Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
