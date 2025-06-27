package com.fiprod.civicfix;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Patterns;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.*;

public class LoginActivity extends AppCompatActivity {

    EditText emailET, passwordET;
    Button loginBtn;
    ImageView togglePasswordVisibility;
    TextView forgotPassBtn, registerBtn;
    FirebaseAuth mAuth;

    boolean isPasswordVisible = false;

    DatabaseReference dbRef;

    @Override
    protected void onStart() {
        super.onStart();
        mAuth = FirebaseAuth.getInstance();
        dbRef = FirebaseDatabase.getInstance().getReference();

        if (mAuth.getCurrentUser() != null) {
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }
    }

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

        setContentView(R.layout.activity_login);

        emailET = findViewById(R.id.email_input);
        passwordET = findViewById(R.id.password_input);
        loginBtn = findViewById(R.id.login_button);
        forgotPassBtn = findViewById(R.id.forgot_password);
        registerBtn = findViewById(R.id.signup_now);
        togglePasswordVisibility = findViewById(R.id.togglePasswordVisibility);

        dbRef = FirebaseDatabase.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();

        loginBtn.setOnClickListener(v -> loginUser());

        forgotPassBtn.setOnClickListener(v -> {
            String email = emailET.getText().toString().trim();

            if (!isConnected(this)) {
                Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
                return;
            }

            if (email.isEmpty()) {
                Toast.makeText(this, "Enter email", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
                return;
            }

            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "If this email is registered, you will receive a password reset email.", Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e -> {
                        String errorMessage = e.getMessage();
                        if (errorMessage != null && errorMessage.contains("no user record")) {
                            Toast.makeText(this, "No account found with this email address.", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to send reset email. Please try again.", Toast.LENGTH_SHORT).show();
                        }
                    });
        });

        registerBtn.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            finish();
        });

        togglePasswordVisibility.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye);
            } else {
                passwordET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_off);
            }
            passwordET.setSelection(passwordET.getText().length());
        });
    }

    private void loginUser() {
        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();

        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        if (!isValidInput(email, password)) {
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();

                if (user != null && user.isEmailVerified()) {
                    String uid = user.getUid();

                    dbRef.child("users").child(uid).get().addOnSuccessListener(snapshot -> {
                        if (snapshot.exists()) {
                            String role = snapshot.child("role").getValue(String.class);

                            if ("Government Employee".equalsIgnoreCase(role)) {
                                Boolean verifiedDoc = snapshot.child("verifiedDoc").getValue(Boolean.class);
                                Toast.makeText(this, "hi" + verifiedDoc, Toast.LENGTH_LONG).show();

                                if (verifiedDoc != null && verifiedDoc) {
                                    updateUserVerifiedInRealtimeDB(uid);
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Please wait while we verify your documents.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            } else {
                                updateUserVerifiedInRealtimeDB(uid);
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "User profile not found.", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    }).addOnFailureListener(e -> {
                        Toast.makeText(this, "Failed to retrieve user data.", Toast.LENGTH_SHORT).show();
                        mAuth.signOut();
                    });

                } else {
                    Toast.makeText(this, "Please verify your email first.", Toast.LENGTH_LONG).show();
                    mAuth.signOut();
                }
            } else {
                Toast.makeText(this, "Login Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserVerifiedInRealtimeDB(String uid) {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }

        dbRef.child("users").child(uid).child("verified").setValue(true)
                .addOnSuccessListener(aVoid -> {
                    // Updated successfully
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update verification status.", Toast.LENGTH_SHORT).show();
                });
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private boolean isValidInput(String email, String password) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.isEmpty()) {
            Toast.makeText(this, "Password is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return false;
        }

        return true;
    }
}
