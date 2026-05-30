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
    // Construct the Auth URL with a redirect back to our app's deep link
    private static final String REDIRECT_URI = "cocotrade://auth-callback";
    private static final String AUTH_URL = "https://" + AUTH_DOMAIN + "/__/auth/handler?apiKey=AIzaSyDo3ff8V73OkrZ5Hh2r-DaLltBX3uyPtQc&appName=%5BDEFAULT%5D&authType=signInWithPopup&providerId=google.com&scopes=profile%2Cemail&redirect_uri=" + Uri.encode(REDIRECT_URI);

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
        Log.d(TAG, "Launching Custom Tabs for Auth: " + AUTH_URL);
        CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
        CustomTabsIntent customTabsIntent = builder.build();
        customTabsIntent.launchUrl(this, Uri.parse(AUTH_URL));
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }

    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && REDIRECT_URI.equals(data.getScheme() + "://" + data.getHost())) {
            Log.d(TAG, "Deep link received: " + data.toString());
            // In a real Firebase flow, the redirect would contain a token or session info.
            // For simplicity in this secure-window bypass, we wait for Firebase Auth to sync the state.
            checkFirebaseAuth();
        }
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
