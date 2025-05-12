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
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    EditText emailET, passwordET, firstNameET, lastNameET, phoneET, confirmPasswordET;
    EditText officeNameET, designationET, idCardNumberET;
    Spinner roleSpinner;
    Button signupBtn;
    TextView loginNow, verifyMail;

    FirebaseAuth mAuth;
    FirebaseFirestore db;

    ImageView togglePassword;
    ImageView toggleConfirmPassword;

    private boolean isPasswordVisible = false;
    private boolean isConfirmPasswordVisible = false;

    String selectedRole = "Citizen"; // default

    @SuppressLint("MissingInflatedId")
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

        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailET = findViewById(R.id.email);
        passwordET = findViewById(R.id.password);
        confirmPasswordET = findViewById(R.id.confirm_password);
        firstNameET = findViewById(R.id.first_name);
        lastNameET = findViewById(R.id.last_name);
        phoneET = findViewById(R.id.phone_number);
        signupBtn = findViewById(R.id.signup_button);
        loginNow = findViewById(R.id.loginNow);
        verifyMail = findViewById(R.id.verifyMail);
        verifyMail.setVisibility(View.GONE);

        togglePassword = findViewById(R.id.toggle_password);
        toggleConfirmPassword = findViewById(R.id.toggle_confirm_password);

        roleSpinner = findViewById(R.id.account_type_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_item,
                new String[]{"Citizen", "Government Employee"});
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        roleSpinner.setAdapter(adapter);

        idCardNumberET = findViewById(R.id.id_card_number);
        officeNameET = findViewById(R.id.office_name);
        designationET = findViewById(R.id.designation);

        roleSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                selectedRole = parent.getItemAtPosition(position).toString();
                toggleGovFields(selectedRole.equals("Government Employee"));
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        signupBtn.setOnClickListener(v -> registerUser());

        verifyMail.setOnClickListener(v -> resendVerificationEmail());

        loginNow.setOnClickListener(v -> {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
        });

        togglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                passwordET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye); // open eye icon
            } else {
                passwordET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                togglePassword.setImageResource(R.drawable.ic_eye_off); // closed eye icon
            }
            passwordET.setSelection(passwordET.getText().length());
        });
        toggleConfirmPassword.setOnClickListener(v -> {
            isConfirmPasswordVisible = !isConfirmPasswordVisible;
            if (isConfirmPasswordVisible) {
                confirmPasswordET.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
                toggleConfirmPassword.setImageResource(R.drawable.ic_eye); // open eye icon
            } else {
                confirmPasswordET.setTransformationMethod(PasswordTransformationMethod.getInstance());
                toggleConfirmPassword.setImageResource(R.drawable.ic_eye_off); // closed eye icon
            }
            confirmPasswordET.setSelection(confirmPasswordET.getText().length());
        });
    }

    private void toggleGovFields(boolean isGov) {
        int visibility = isGov ? View.VISIBLE : View.GONE;
        idCardNumberET.setVisibility(visibility);
        officeNameET.setVisibility(visibility);
        designationET.setVisibility(visibility);
    }

    private void registerUser() {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            return;
        }

        String email = emailET.getText().toString().trim();
        String password = passwordET.getText().toString().trim();
        String confirmPassword = confirmPasswordET.getText().toString().trim();
        String firstName = firstNameET.getText().toString().trim();
        String lastName = lastNameET.getText().toString().trim();
        String phone = phoneET.getText().toString().trim();
        String office;
        String designation;
        String idCardNumber;

        if (selectedRole.equals("Government Employee")) {
            office = officeNameET.getText().toString().trim();
            designation = designationET.getText().toString().trim();
            idCardNumber = idCardNumberET.getText().toString().trim();
        } else {
            office = "";
            designation = "";
            idCardNumber = "";
        }

        if (!isValidRegistrationInput(email, password, confirmPassword, firstName, lastName, phone, selectedRole, idCardNumber, office, designation)) {
            return;
        }

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    user.sendEmailVerification()
                            .addOnSuccessListener(unused -> {
                                Toast.makeText(this, "Verification email sent. Please verify before logging in.", Toast.LENGTH_LONG).show();
                                verifyMail.setVisibility(View.VISIBLE);

                                String uid = user.getUid();
                                Map<String, Object> userMap = new HashMap<>();
                                userMap.put("email", email.toLowerCase());
                                userMap.put("firstName", firstName);
                                userMap.put("lastName", lastName);
                                userMap.put("phone", phone);
                                userMap.put("role", selectedRole);
                                userMap.put("loginDate", FieldValue.serverTimestamp());
                                userMap.put("verified", false);

                                if (selectedRole.equals("Government Employee")) {
                                    userMap.put("officeName", office);
                                    userMap.put("designation", designation);
                                    userMap.put("idCardNumber", idCardNumber);
                                }

                                saveUserData(uid, userMap);

                                mAuth.signOut(); // Prevent auto-login
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                            );
                }
            } else {
                Toast.makeText(this, "Registration Failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void resendVerificationEmail() {
        if (!isConnected(this)) {
            Toast.makeText(this, "No internet connection.", Toast.LENGTH_LONG).show();
            return;
        }
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null && !user.isEmailVerified()) {
            user.sendEmailVerification()
                    .addOnSuccessListener(unused ->
                            Toast.makeText(this, "Verification email resent.", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to resend verification email.", Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this, "No user found or already verified.", Toast.LENGTH_SHORT).show();
        }
    }

    private void saveUserData(String uid, Map<String, Object> userMap) {
        db.collection("users").document(uid).set(userMap).addOnSuccessListener(unused -> {
            // Don't go to MainActivity now
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Failed to save user data.", Toast.LENGTH_SHORT).show()
        );
    }

    private boolean isConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnected();
    }

    private boolean isValidRegistrationInput(String email, String password, String confirmPassword,
                                             String firstName, String lastName, String phone,
                                             String role, String idCardNumber,
                                             String office, String designation) {
        if (email.isEmpty()) {
            Toast.makeText(this, "Email is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Invalid email format", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (firstName.isEmpty()) {
            Toast.makeText(this, "First name is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (lastName.isEmpty()) {
            Toast.makeText(this, "Last name is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (phone.isEmpty()) {
            Toast.makeText(this, "Phone number is required", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (!phone.matches("^01[3-9]\\d{8}$")) {
            Toast.makeText(this, "Invalid Bangladeshi phone number", Toast.LENGTH_SHORT).show();
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

        if (!password.equals(confirmPassword)) {
            Toast.makeText(this, "Passwords do not match!", Toast.LENGTH_SHORT).show();
            return false;
        }

        if (role.equals("Government Employee")) {
            if (office.isEmpty()) {
                Toast.makeText(this, "Office name is required", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (designation.isEmpty()) {
                Toast.makeText(this, "Designation is required", Toast.LENGTH_SHORT).show();
                return false;
            }

            if (idCardNumber.isEmpty()) {
                Toast.makeText(this, "ID card number is required", Toast.LENGTH_SHORT).show();
                return false;
            }
        }

        return true;
    }
}
