package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.Map;

import static android.app.Activity.RESULT_OK;

public class PostFragment extends Fragment {

    private static final String TAG = "PostFragment";
    private PlayerView videoPreview;
    private ExoPlayer mPlayer;
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
        if (videoPreview == null) return;

        if (mPlayer != null) {
            mPlayer.release();
        }

        mPlayer = new ExoPlayer.Builder(requireContext()).build();
        mPlayer.setMediaItem(MediaItem.fromUri(selectedVideoUri));
        mPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        mPlayer.prepare();
        mPlayer.play();

        videoPreview.setPlayer(mPlayer);
        videoPreview.setVisibility(View.VISIBLE);

        btnUpload.setEnabled(true);
        statusText.setText("Video selected");
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("UserVideos");

        videoPreview = view.findViewById(R.id.video_preview);

        // Apply rounded corners to preview
        videoPreview.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View view, Outline outline) {
                float radius = view.getContext().getResources().getDisplayMetrics().density * 24;
                outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
            }
        });
        videoPreview.setClipToOutline(true);

        btnRecord = view.findViewById(R.id.btn_record_video);
        btnSelect = view.findViewById(R.id.btn_select_video);
        btnUpload = view.findViewById(R.id.btn_upload_video);
        progressBar = view.findViewById(R.id.upload_progress);
        statusText = view.findViewById(R.id.status_text);

        etName = view.findViewById(R.id.et_post_name);
        etMobile = view.findViewById(R.id.et_post_mobile);
        etArea = view.findViewById(R.id.et_post_area);
        etCost = view.findViewById(R.id.et_post_cost);
        etCaption = view.findViewById(R.id.et_post_caption);

        btnRecord.setOnClickListener(v -> {
            Intent intent = new Intent(android.provider.MediaStore.ACTION_VIDEO_CAPTURE);
            intent.putExtra(android.provider.MediaStore.EXTRA_DURATION_LIMIT, 20);
            intent.putExtra(android.provider.MediaStore.EXTRA_VIDEO_QUALITY, 0); // 0 for low quality/compression
            recordVideoLauncher.launch(intent);
        });

        btnSelect.setOnClickListener(v -> pickVideoLauncher.launch("video/*"));
        btnUpload.setOnClickListener(v -> uploadVideo());

        return view;
    }

    private void uploadVideo() {
        if (selectedVideoUri == null) return;

        String name = etName.getText().toString().trim();
        if (name.isEmpty()) {
            Toast.makeText(getContext(), "Name is required", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
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
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> progressBar.setProgress(progress));
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        saveToFirebase(url, user.getUid());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                progressBar.setVisibility(View.GONE);
                                btnUpload.setEnabled(true);
                                Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show();
                            });
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveToFirebase(String url, String userId) {
        String videoId = mDatabase.push().getKey();
        FirebaseUser user = mAuth.getCurrentUser();
        String profileName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("objectUrl", url);
        data.put("uploadedBy", userId);
        data.put("authorName", profileName);
        data.put("createdAt", System.currentTimeMillis());
        data.put("name", etName.getText().toString().trim());
        data.put("mobile", etMobile.getText().toString().trim());
        data.put("area", etArea.getText().toString().trim());
        data.put("cost", etCost.getText().toString().trim());
        data.put("caption", etCaption.getText().toString().trim());

        if (videoId != null) {
            mDatabase.child(videoId).setValue(data).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
                    // Clear form or navigate to feed
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).replaceFragment(new FeedFragment());
                        // Update bottom nav selection
                        BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                        if (nav != null) nav.setSelectedItemId(R.id.nav_feed);
                    }
                } else {
                    progressBar.setVisibility(View.GONE);
                    btnUpload.setEnabled(true);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
    }
}
