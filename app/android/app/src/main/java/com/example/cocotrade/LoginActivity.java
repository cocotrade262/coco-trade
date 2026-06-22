package com.example.cocotrade;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.Signature;
import androidx.annotation.NonNull;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.ForegroundColorSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.appcompat.app.AlertDialog;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private ProgressBar progressBar;
    private CardView btnGoogleCustom;

    private final ActivityResultLauncher<Intent> googleSignInLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "ActivityResult code: " + result.getResultCode());
                Intent data = result.getData();
                if (data != null) {
                    Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
                    handleSignInResult(task);
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnGoogleCustom.setEnabled(true);
                    String msg = "Google Sign In failed (No data). Code: " + result.getResultCode();
                    Log.e(TAG, msg);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        mAuth = FirebaseAuth.getInstance();
        progressBar = findViewById(R.id.progress_bar);
        btnGoogleCustom = findViewById(R.id.btn_google_custom);

        setupTermsAgreementText();

        String webClientId = getString(R.string.default_web_client_id);

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(webClientId)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            updateUI(currentUser);
        }

        btnGoogleCustom.setOnClickListener(v -> signIn());
    }

    private void signIn() {
        progressBar.setVisibility(View.VISIBLE);
        btnGoogleCustom.setEnabled(false);
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        googleSignInLauncher.launch(signInIntent);
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            if (account != null && account.getIdToken() != null) {
                firebaseAuthWithGoogle(account.getIdToken());
            } else {
                throw new ApiException(new com.google.android.gms.common.api.Status(13, "Account or ID Token is null"));
            }
        } catch (ApiException e) {
            Log.e(TAG, "signInResult:failed code=" + e.getStatusCode(), e);
            progressBar.setVisibility(View.GONE);
            btnGoogleCustom.setEnabled(true);

            String fingerprint = getCertificateFingerprint();
            String errorMessage = "Google sign in failed (Code " + e.getStatusCode() + ")\n";
            errorMessage += "SHA1: " + fingerprint + "\n";

            if (e.getStatusCode() == 12500 || e.getStatusCode() == 10) {
                errorMessage += "Check if this SHA1 is in Firebase Settings.";
            }

            Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show();
        }
    }

    private String getCertificateFingerprint() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), PackageManager.GET_SIGNATURES);
            for (Signature signature : info.signatures) {
                MessageDigest md = MessageDigest.getInstance("SHA1");
                md.update(signature.toByteArray());
                byte[] digest = md.digest();
                StringBuilder hexString = new StringBuilder();
                for (byte b : digest) {
                    String appendString = Integer.toHexString(0xFF & b);
                    if (appendString.length() == 1) hexString.append("0");
                    hexString.append(appendString).append(":");
                }
                String result = hexString.toString();
                return result.substring(0, result.length() - 1).toUpperCase();
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting fingerprint", e);
        }
        return "Unknown";
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressBar.setVisibility(View.GONE);
                    btnGoogleCustom.setEnabled(true);
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        updateUI(user);
                    } else {
                        Log.e(TAG, "Firebase auth with Google failed", task.getException());
                        String error = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Toast.makeText(this, "Firebase Auth failed: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void setupTermsAgreementText() {
        TextView tvAgreement = findViewById(R.id.tv_terms_agreement);
        String fullText = "By signing in, you agree to our Terms and Conditions and Privacy Policy";
        SpannableString ss = new SpannableString(fullText);

        ClickableSpan termsSpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showTermsDialogWithClose();
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };

        ClickableSpan privacySpan = new ClickableSpan() {
            @Override
            public void onClick(@NonNull View widget) {
                showPrivacyDialog();
            }
            @Override
            public void updateDrawState(@NonNull TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(true);
            }
        };

        int termsStart = fullText.indexOf("Terms and Conditions");
        int termsEnd = termsStart + "Terms and Conditions".length();
        int privacyStart = fullText.indexOf("Privacy Policy");
        int privacyEnd = privacyStart + "Privacy Policy".length();

        ss.setSpan(termsSpan, termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#3880ff")), termsStart, termsEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(privacySpan, privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        ss.setSpan(new ForegroundColorSpan(Color.parseColor("#3880ff")), privacyStart, privacyEnd, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

        tvAgreement.setText(ss);
        tvAgreement.setMovementMethod(LinkMovementMethod.getInstance());
        tvAgreement.setHighlightColor(Color.TRANSPARENT);
    }

    private void updateUI(FirebaseUser user) {
        if (user != null) {
            SharedPreferences prefs = getSharedPreferences("cocotrade_prefs", Context.MODE_PRIVATE);
            boolean accepted = prefs.getBoolean("terms_accepted_" + user.getUid(), false);

            if (accepted) {
                proceedToMain(user);
            } else {
                showTermsDialog(user);
            }
        }
    }

    private void showTermsDialog(FirebaseUser user) {
        String termsText = getTermsText();

        new AlertDialog.Builder(this)
                .setTitle("Terms and Conditions")
                .setMessage(termsText)
                .setCancelable(false)
                .setPositiveButton("Accept", (dialog, which) -> {
                    SharedPreferences prefs = getSharedPreferences("cocotrade_prefs", Context.MODE_PRIVATE);
                    prefs.edit().putBoolean("terms_accepted_" + user.getUid(), true).apply();
                    proceedToMain(user);
                })
                .setNegativeButton("Decline", (dialog, which) -> {
                    mAuth.signOut();
                    Toast.makeText(this, "You must accept the terms to use the app", Toast.LENGTH_LONG).show();
                })
                .show();
    }

    private void showTermsDialogWithClose() {
        new AlertDialog.Builder(this)
                .setTitle("Terms and Conditions")
                .setMessage(getTermsText())
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void showPrivacyDialog() {
        String privacyText = "COCOTRADE – PRIVACY POLICY\n\n" +
                "Last Updated: June 22, 2026\n\n" +
                "This Privacy Policy describes how CocoTrade collects, uses, and shares your personal information when you use our mobile application.\n\n" +
                "1. Information We Collect\n" +
                "Account Information: We use Google Sign-In to authenticate you. When you sign in, we receive your basic profile information such as your name, email address, and profile picture URL.\n" +
                "User-Generated Content: We collect the videos you upload to our platform, along with any metadata you provide, such as captions, location, and contact information.\n\n" +
                "2. How We Use Your Information\n" +
                "To provide and maintain our service.\n" +
                "To authenticate your identity and link your uploaded content to your account.\n\n" +
                "3. Sharing of Information\n" +
                "Any content you upload to CocoTrade, including your name, location, and contact information if provided, is shared publicly and can be viewed by all users of the application. We do not sell your personal data to third parties.\n\n" +
                "4. Data Storage\n" +
                "We use Firebase (a Google service) for user authentication and database management. Videos are hosted using Cloudinary.\n\n" +
                "5. Content Deletion\n" +
                "Users can delete their own uploaded videos. If you wish to delete your account, contact us at cocotrade262@gmail.com.\n\n" +
                "6. Contact Us\n" +
                "If you have any questions about this Privacy Policy, please contact us at cocotrade262@gmail.com.";

        new AlertDialog.Builder(this)
                .setTitle("Privacy Policy")
                .setMessage(privacyText)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }

    private String getTermsText() {
        return "COCOTRADE – TERMS AND CONDITIONS\n\n" +
                "Last Updated: June 22, 2026\n\n" +
                "Welcome to CocoTrade. By downloading, installing, or using our mobile application, you agree to be bound by these Terms and Conditions. If you do not agree with any part of these terms, you must immediately stop using the app.\n\n" +
                "1. Informational Purpose Only (No Financial Responsibility)\n" +
                "Purely Informational: CocoTrade is strictly an information-sharing platform. We do not facilitate, handle, process, or guarantee any financial transactions.\n" +
                "No Financial Liability: We are not responsible for any fraud, scams, financial losses, or bad deals that may occur if you contact or trade with other users outside of this app.\n" +
                "We Don't Pay for Losses: Under no circumstances will CocoTrade, its developers, or owners be liable to pay you anything for monetary or physical losses resulting from your use of the app.\n\n" +
                "2. User Data & Personal Information Responsibility\n" +
                "Voluntary Disclosure: When you use this app, you may choose to share your mobile number, name, personal details, location, place, or videos.\n" +
                "User's Sole Risk: Disclosing this information is 100% your own responsibility.\n" +
                "No Privacy Liability: We are not responsible for how other users or third parties use your phone number, location, or personal details once you post them publicly on the platform.\n\n" +
                "3. No Guarantee of Correct Information\n" +
                "\"As-Is\" Data: We do not verify, screen, or guarantee the accuracy, truthfulness, or correctness of the information shared by users.\n\n" +
                "4. Strict Video Content Rules\n" +
                "Coconut Content Only: Only coconut-related videos are permitted.\n" +
                "No Objectionable Content: You must not upload any illegal, abusive, hateful, defamatory, or pornographic content.\n" +
                "Right to Terminate: We reserve the right to delete any video and permanently ban any user who violates these content rules.\n\n" +
                "5. Limitation of Liability\n" +
                "To the maximum extent permitted by applicable law, CocoTrade shall not be liable for any direct, indirect, incidental, or consequential damages resulting from the use or the inability to use this platform.";
    }

    private void proceedToMain(FirebaseUser user) {
        Toast.makeText(this, "Welcome " + user.getDisplayName(), Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
