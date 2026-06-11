package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private View btnRecord, btnSelect;
    private Button btnUpload;
    private ProgressBar progressBar;
    private TextView statusText;
    private EditText etName, etMobile, etArea, etCost, etCaption;
    private Uri selectedVideoUri;

    private DatabaseReference mDatabase;
    private FirebaseAuth mAuth;

    private final ActivityResultLauncher<String> pickVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedVideoUri = uri;
                    onVideoSelected();
                }
            }
    );

    private final ActivityResultLauncher<Intent> recordVideoLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedVideoUri = result.getData().getData();
                    onVideoSelected();
                }
            }
    );

    private void onVideoSelected() {
        videoPreview.setVideoURI(selectedVideoUri);
        videoPreview.setVisibility(View.VISIBLE);
        videoPreview.setOnPreparedListener(mp -> {
            mp.setLooping(true);
            float videoRatio = mp.getVideoWidth() / (float) mp.getVideoHeight();
            float screenRatio = videoPreview.getWidth() / (float) videoPreview.getHeight();
            float scale = videoRatio / screenRatio;
            if (scale >= 1f) {
                videoPreview.setScaleX(scale);
            } else {
                videoPreview.setScaleY(1f / scale);
            }
            videoPreview.start();
        });
        btnUpload.setEnabled(true);
        statusText.setText("Video selected");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_video_upload);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("UserVideos");

        videoPreview = findViewById(R.id.video_preview);
        btnRecord = findViewById(R.id.btn_record_video);
        btnSelect = findViewById(R.id.btn_select_video);
        btnUpload = findViewById(R.id.btn_upload_video);
        progressBar = findViewById(R.id.upload_progress);
        statusText = findViewById(R.id.status_text);

        etName = findViewById(R.id.et_post_name);
        etMobile = findViewById(R.id.et_post_mobile);
        etArea = findViewById(R.id.et_post_area);
        etCost = findViewById(R.id.et_post_cost);
        etCaption = findViewById(R.id.et_post_caption);

        btnRecord.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 30);
            recordVideoLauncher.launch(intent);
        });

        btnSelect.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        btnUpload.setOnClickListener(v -> uploadVideo());
    }

    private void uploadVideo() {
        if (selectedVideoUri == null) return;

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(this, "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        btnUpload.setEnabled(false);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText("Uploading...");

        MediaManager.get().upload(selectedVideoUri)
                .unsigned("ml_default")
                .option("resource_type", "video")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {
                        int progress = (int) ((bytes * 100) / totalBytes);
                        runOnUiThread(() -> progressBar.setProgress(progress));
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        saveToFirebase(url, user.getUid());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            btnUpload.setEnabled(true);
                            Toast.makeText(VideoUploadActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                        });
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveToFirebase(String url, String userId) {
        String videoId = mDatabase.push().getKey();

        Map<String, Object> data = new HashMap<>();
        data.put("objectUrl", url);
        data.put("uploadedBy", userId);
        data.put("createdAt", System.currentTimeMillis());
        data.put("name", etName.getText().toString().trim());
        data.put("mobile", etMobile.getText().toString().trim());
        data.put("area", etArea.getText().toString().trim());
        data.put("cost", etCost.getText().toString().trim());
        data.put("caption", etCaption.getText().toString().trim());

        if (videoId != null) {
            mDatabase.child(videoId).setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(this, "Success!", Toast.LENGTH_SHORT).show();
                    finish();
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                }
            });
        }
    }
}
