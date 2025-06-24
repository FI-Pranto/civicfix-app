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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class LoginActivity extends AppCompatActivity {

    EditText emailET, passwordET;
    Button loginBtn;
    ImageView togglePasswordVisibility;
    TextView forgotPassBtn, registerBtn;
    FirebaseAuth mAuth;

    boolean isPasswordVisible = false;

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

            FirebaseAuth mAuth = FirebaseAuth.getInstance();


            mAuth.sendPasswordResetEmail(email)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(this, "If this email is registered, you will receive a password reset email.", Toast.LENGTH_LONG).show();
                    })
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
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye); // change to open eye icon
            } else {
                passwordET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePasswordVisibility.setImageResource(R.drawable.ic_eye_off); // change back to eye-off icon
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
                    FirebaseFirestore db = FirebaseFirestore.getInstance();

                    db.collection("users").document(uid).get().addOnSuccessListener(document -> {
                        if (document.exists()) {
                            String role = document.getString("role");

                            if ("Government Employee".equalsIgnoreCase(role)) {
                                Boolean verifiedDoc = document.getBoolean("verifiedDoc");
                                Toast.makeText(this, "hi"+verifiedDoc, Toast.LENGTH_LONG).show();
                                if (verifiedDoc != null && verifiedDoc) {
                                    updateUserVerifiedInFirestore();
                                    startActivity(new Intent(this, MainActivity.class));
                                    finish();
                                } else {
                                    Toast.makeText(this, "Please wait while we verify your documents.", Toast.LENGTH_LONG).show();
                                    mAuth.signOut();
                                }
                            } else {
                                updateUserVerifiedInFirestore();
                                startActivity(new Intent(this, MainActivity.class));
                                finish();
                            }
                        } else {
                            Toast.makeText(this, "User profile not found in Firestore.", Toast.LENGTH_SHORT).show();
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

    private void updateUserVerifiedInFirestore() {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection. Please enable internet.", Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        db.collection("users").document(uid)
                .update("verified", true)
                .addOnSuccessListener(aVoid -> {
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this, "Failed to update verification status.", Toast.LENGTH_SHORT).show();
                });
    }


    //  Internet checker
    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    //email and password checker
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
