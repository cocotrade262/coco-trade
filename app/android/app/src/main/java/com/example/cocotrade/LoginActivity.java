package com.example.cocotrade;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.CookieManager;
import android.webkit.WebResourceRequest;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private FirebaseAuth mAuth;
    private ProgressBar progressBar;
    private Button btnSignIn;
    private WebView webView;
    private LinearLayout loginUi;

    // Use the Firebase project's Auth Domain
    private static final String AUTH_DOMAIN = "cocotrade-fc1a5.firebaseapp.com";
    private static final String AUTH_URL = "https://" + AUTH_DOMAIN + "/__/auth/handler?apiKey=AIzaSyDo3ff8V73OkrZ5Hh2r-DaLltBX3uyPtQc&appName=%5BDEFAULT%5D&authType=signInWithPopup&providerId=google.com&scopes=profile%2Cemail";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_bar);
        btnSignIn = findViewById(R.id.btn_google_signin);
        webView = findViewById(R.id.webview);
        loginUi = findViewById(R.id.login_ui);

        setupWebView();

        // Check if user is already signed in
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }

        btnSignIn.setOnClickListener(v -> startWebAuth());
    }

    private void setupWebView() {
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setDomStorageEnabled(true);
        webView.getSettings().setSupportMultipleWindows(true);
        webView.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);

        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(webView, true);

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                progressBar.setVisibility(View.VISIBLE);
                Log.d(TAG, "Page started: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                progressBar.setVisibility(View.GONE);
                Log.d(TAG, "Page finished: " + url);

                // If the URL contains signs of a successful auth redirect or callback
                // Note: In a real Firebase setup with popup, we'd listen for the redirect back to the app or a success page.
                // Since this is a simple implementation, we check for the Firebase user periodically or on redirect.
                checkFirebaseAuth();
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                String url = request.getUrl().toString();
                Log.d(TAG, "Loading URL: " + url);
                return false;
            }
        });
    }

    private void startWebAuth() {
        loginUi.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(AUTH_URL);
    }

    private void checkFirebaseAuth() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            updateUI(user);
        }
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            runOnUiThread(() -> {
                Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish();
            });
        }
    }

    @Override
    public void onBackPressed() {
        if (webView.getVisibility() == View.VISIBLE) {
            webView.setVisibility(View.GONE);
            loginUi.setVisibility(View.VISIBLE);
            webView.stopLoading();
        } else {
            super.onBackPressed();
        }
    }
}
