package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

public class VideoUploadActivity extends AppCompatActivity {

    private static final String TAG = "VideoUploadActivity";
    private VideoView videoPreview;
    private Button btnSelect, btnUpload;
    private ProgressBar progressBar;
    private TextView statusText;
    private Uri selectedVideoUri;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedVideoUri = uri;
                    videoPreview.setVideoURI(selectedVideoUri);
                    videoPreview.start();
                    btnUpload.setEnabled(true);
                    statusText.setText("Video selected: " + selectedVideoUri.getLastPathSegment());
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_upload);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference();

        videoPreview = findViewById(R.id.video_preview);
        btnSelect = findViewById(R.id.btn_select_video);
        btnUpload = findViewById(R.id.btn_upload_video);
        progressBar = findViewById(R.id.upload_progress);
        statusText = findViewById(R.id.status_text);

        btnSelect.setOnClickListener(v -> selectVideo());
        btnUpload.setOnClickListener(v -> uploadVideo());
    }

    private void selectVideo() {
        pickVideoLauncher.launch("video/mp4");
    }

    private void uploadVideo() {
        if (selectedVideoUri == null) return;

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            Toast.makeText(this, "Please sign in with Google first.", Toast.LENGTH_SHORT).show();
            Intent intent = new Intent(VideoUploadActivity.this, LoginActivity.class);
            startActivity(intent);
            finish();
            return;
        }

        btnUpload.setEnabled(false);
        btnSelect.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Uploading to Cloudinary...");

        MediaManager.get().upload(selectedVideoUri)
                .unsigned("cocotrade_unsigned")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {
                        Log.d(TAG, "Upload started: " + requestId);
                    }

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        runOnUiThread(() -> progressBar.setProgress(progress));
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String videoUrl = (String) resultData.get("secure_url");
                        Log.d(TAG, "Upload success: " + videoUrl);
                        saveVideoMetadataToFirebase(videoUrl, user.getUid());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Cloudinary Error: " + error.getDescription());
                        runOnUiThread(() -> {
                            resetUI();
                            Toast.makeText(VideoUploadActivity.this, "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {
                        Log.d(TAG, "Upload rescheduled");
                    }
                })
                .dispatch();
    }

    private void saveVideoMetadataToFirebase(String url, String userId) {
        runOnUiThread(() -> statusText.setText("Saving metadata to Firebase..."));

        String videoId = mDatabase.child("UserVideos").push().getKey();

        Map<String, Object> videoData = new HashMap<>();
        videoUrlData(videoData, url, userId);

        if (videoId != null) {
            mDatabase.child("UserVideos").child(videoId).setValue(videoData)
                    .addOnSuccessListener(aVoid -> {
                        Log.d(TAG, "Firebase write success");
                        runOnUiThread(() -> {
                            resetUI();
                            Toast.makeText(VideoUploadActivity.this, "Video uploaded and saved!", Toast.LENGTH_LONG).show();
                        });
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Firebase Error: " + e.getMessage());
                        runOnUiThread(() -> {
                            resetUI();
                            Toast.makeText(VideoUploadActivity.this, "Failed to save metadata: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        });
                    });
        }
    }

    private void videoUrlData(Map<String, Object> data, String url, String userId) {
        data.put("videoUrl", url);
        data.put("uploadedBy", userId);
        data.put("timestamp", System.currentTimeMillis());
    }

    private void resetUI() {
        btnUpload.setEnabled(false);
        btnSelect.setEnabled(true);
        progressBar.setVisibility(View.GONE);
        progressBar.setProgress(0);
        statusText.setText("");
        videoPreview.stopPlayback();
        selectedVideoUri = null;
    }
}
