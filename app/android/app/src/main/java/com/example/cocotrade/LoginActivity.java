package com.example.cocotrade;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private Button btnSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);
        progressBar = findViewById(R.id.progress_bar);
        btnSignIn = findViewById(R.id.btn_google_signin);

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }

        btnSignIn.setOnClickListener(v -> signInWithGoogle());
    }

    private void signInWithGoogle() {
        progressBar.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);

        // Web Client ID for your project
        String webClientId = "274853330536-vlt0nphh115t707f1o90q9l56asolp3q.apps.googleusercontent.com";

        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(webClientId)
                .setAutoSelectEnabled(true)
                .build();

        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        Executor executor = Executors.newSingleThreadExecutor();

        credentialManager.getCredentialAsync(this, request, null, executor, new androidx.credentials.CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
            @Override
            public void onResult(GetCredentialResponse result) {
                String idToken = GoogleIdTokenCredential.createFrom(result.getCredential().getData()).getIdToken();
                firebaseAuthWithGoogle(idToken);
            }

            @Override
            public void onError(@NonNull GetCredentialException e) {
                runOnUiThread(() -> {
                    progressBar.setVisibility(View.GONE);
                    btnSignIn.setEnabled(true);
                    Log.e(TAG, "Credential Manager Error", e);
                    Toast.makeText(LoginActivity.this, "Sign in failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    runOnUiThread(() -> {
                        progressBar.setVisibility(View.GONE);
                        btnSignIn.setEnabled(true);
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            updateUI(user);
                        } else {
                            Log.w(TAG, "Firebase Auth Failed", task.getException());
                            Toast.makeText(LoginActivity.this, "Authentication Failed.", Toast.LENGTH_SHORT).show();
                        }
                    });
                });
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
            // Transition to MainActivity (the Capacitor Bridge)
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
