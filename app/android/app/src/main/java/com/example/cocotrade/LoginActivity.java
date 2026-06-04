package com.example.cocotrade;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;
import android.widget.EditText;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Button btnLogin;
    private EditText etEmail, etPassword, etName;
    private TextView tvToggleMode, tvTitle;
    private boolean isLoginMode = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_bar);
        btnLogin = findViewById(R.id.btn_login);
        etEmail = findViewById(R.id.et_email);
        etPassword = findViewById(R.id.et_password);
        etName = findViewById(R.id.et_name);
        tvToggleMode = findViewById(R.id.tv_toggle_mode);
        tvTitle = findViewById(R.id.tv_login_title);

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }

        btnLogin.setOnClickListener(v -> handleAuthAction());
        tvToggleMode.setOnClickListener(v -> toggleAuthMode());
    }

    private void toggleAuthMode() {
        isLoginMode = !isLoginMode;
        if (isLoginMode) {
            tvTitle.setText("Login");
            btnLogin.setText("Login");
            etName.setVisibility(View.GONE);
            tvToggleMode.setText("Don't have an account? Sign Up");
        } else {
            tvTitle.setText("Sign Up");
            btnLogin.setText("Sign Up");
            etName.setVisibility(View.VISIBLE);
            tvToggleMode.setText("Already have an account? Login");
        }
    }

    private void handleAuthAction() {
        if (isLoginMode) {
            handleEmailLogin();
        } else {
            handleEmailSignup();
        }
    }

    private void handleEmailLogin() {
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Email and password required", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            progressBar.setVisibility(View.GONE);
            btnLogin.setEnabled(true);
            if (task.isSuccessful()) {
                updateUI(mAuth.getCurrentUser());
            } else {
                showFriendlyError(task.getException());
            }
        });
    }

    private void handleEmailSignup() {
        String name = etName.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "All fields are required", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show();
            return;
        }

        progressBar.setVisibility(View.VISIBLE);
        btnLogin.setEnabled(false);

        mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                            .setDisplayName(name)
                            .build();

                    user.updateProfile(profileUpdates).addOnCompleteListener(profileTask -> {
                        progressBar.setVisibility(View.GONE);
                        btnLogin.setEnabled(true);
                        updateUI(user);
                    });
                }
            } else {
                progressBar.setVisibility(View.GONE);
                btnLogin.setEnabled(true);
                showFriendlyError(task.getException());
            }
        });
    }

    private void showFriendlyError(Exception exception) {
        String message = "Authentication failed";
        if (exception instanceof FirebaseAuthException) {
            String errorCode = ((FirebaseAuthException) exception).getErrorCode();
            switch (errorCode) {
                case "ERROR_INVALID_EMAIL":
                    message = "The email address is badly formatted.";
                    break;
                case "ERROR_WRONG_PASSWORD":
                    message = "The password you entered is incorrect.";
                    break;
                case "ERROR_USER_NOT_FOUND":
                    message = "No account found with this email.";
                    break;
                case "ERROR_EMAIL_ALREADY_IN_USE":
                    message = "This email is already registered.";
                    break;
                case "ERROR_WEAK_PASSWORD":
                    message = "The password is too weak.";
                    break;
                default:
                    message = exception.getMessage();
            }
        } else {
            message = exception != null ? exception.getMessage() : "Unknown error";
        }
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            });
        }
    }
}
