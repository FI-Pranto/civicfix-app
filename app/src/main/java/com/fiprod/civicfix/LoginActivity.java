package com.fiprod.civicfix;

import android.annotation.SuppressLint;
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

public class LoginActivity extends AppCompatActivity {

    EditText emailET, passwordET;
    Button loginBtn, forgotPassBtn, registerBtn;
    FirebaseAuth mAuth;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        loginBtn = findViewById(R.id.loginButton);
        forgotPassBtn = findViewById(R.id.forgotPassword);
        registerBtn = findViewById(R.id.registerRedirect);

        loginBtn.setOnClickListener(v -> loginUser());

        forgotPassBtn.setOnClickListener(v -> {
            String email = emailET.getText().toString().trim();
            if (!isConnected(this)) {
                Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
                return;
            }
            if (!email.isEmpty()) {
                mAuth.sendPasswordResetEmail(email).addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Reset email sent!", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
            }
        });

        registerBtn.setOnClickListener(v ->
                startActivity(new Intent(this, RegisterActivity.class)));
    }

    private void loginUser() {
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();

        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    // 🔌 Internet checker
    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }
}
