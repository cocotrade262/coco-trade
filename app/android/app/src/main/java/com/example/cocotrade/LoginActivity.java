package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.browser.customtabs.CustomTabsIntent;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Button btnSignIn;

    // Use the Firebase project's Auth Domain
    private static final String AUTH_DOMAIN = "cocotrade-fc1a5.firebaseapp.com";
    // Use the web app as the authentication gateway
    private static final String WEB_AUTH_URL = "https://cocotrade262.github.io/#/tabs/account?native=true";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_bar);
        btnSignIn = findViewById(R.id.btn_google_signin);

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }

        btnSignIn.setOnClickListener(v -> startCustomTabsAuth());
    }

    private void startCustomTabsAuth() {
        Log.d(TAG, "Launching Custom Tabs for Auth: " + WEB_AUTH_URL);
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(WEB_AUTH_URL));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "cocotrade".equals(data.getScheme())) {
            Log.d(TAG, "Deep link received: " + data.toString());
            String token = data.getQueryParameter("token");
            if (token != null) {
                firebaseAuthWithToken(token);
            } else {
                checkFirebaseAuth();
            }
        }
    }

    private void firebaseAuthWithToken(String token) {
        progressBar.setVisibility(View.VISIBLE);
        btnSignIn.setEnabled(false);

        mAuth.signInWithCustomToken(token).addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                updateUI(mAuth.getCurrentUser());
            } else {
                // If custom token fails, try standard credential (token might be ID token)
                // Note: Firebase Custom Token is different from ID Token.
                // But we can also use signInWithIdToken if we had a credential.
                // For now, assume it's a custom token or fallback to sync.
                Log.w(TAG, "Custom Token Sign-in failed", task.getException());
                checkFirebaseAuth();
            }
        });
    }

    private void checkFirebaseAuth() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
        } else {
            // Give it a small delay as state sync might take a second after the redirect
            btnSignIn.postDelayed(() -> {
                FirebaseUser retryUser = mAuth.getCurrentUser();
                if (retryUser != null) updateUI(retryUser);
            }, 2000);
        }
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
