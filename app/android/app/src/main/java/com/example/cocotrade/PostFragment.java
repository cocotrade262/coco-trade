package com.example.cocotrade;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.util.Log;
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
import androidx.media3.common.MimeTypes;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.transformer.Composition;
import androidx.media3.transformer.ExportException;
import androidx.media3.transformer.ExportResult;
import androidx.media3.transformer.Transformer;
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
    private ProgressBar progressBar, pbOverlayProgress;
    private TextView statusText, tvOverlayText;
    private EditText etName, etMobile, etArea, etCost, etCaption;
    private Uri selectedVideoUri;
    private View layoutOptimizationOverlay;

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

    private long getFileSize(Uri uri) {
        Cursor cursor = requireContext().getContentResolver().query(uri, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE);
            long size = cursor.getLong(sizeIndex);
            cursor.close();
            return size;
        }
        return 0;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance("https://cocotrade-fc1a5-default-rtdb.firebaseio.com").getReference("videos");

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
        pbOverlayProgress = view.findViewById(R.id.pb_overlay_progress);
        statusText = view.findViewById(R.id.status_text);
        layoutOptimizationOverlay = view.findViewById(R.id.layout_optimization_overlay);
        tvOverlayText = view.findViewById(R.id.tv_overlay_text);

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

    private void onVideoSelected() {
        if (videoPreview == null || selectedVideoUri == null) return;

        if (mPlayer != null) mPlayer.release();
        mPlayer = new ExoPlayer.Builder(requireContext()).build();
        mPlayer.setMediaItem(MediaItem.fromUri(selectedVideoUri));
        mPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == Player.STATE_READY) {
                    long duration = mPlayer.getDuration();
                    if (duration > 20500) { // If > 20s
                        optimizeAndTrimVideo(selectedVideoUri);
                    } else {
                        preparePreview(selectedVideoUri);
                    }
                    mPlayer.removeListener(this);
                }
            }
        });
        mPlayer.prepare();
    }

    private void optimizeAndTrimVideo(Uri inputUri) {
        if (layoutOptimizationOverlay != null) {
            layoutOptimizationOverlay.setVisibility(View.VISIBLE);
            if (tvOverlayText != null) tvOverlayText.setText("Cropping video to 20s...");
            if (pbOverlayProgress != null) pbOverlayProgress.setVisibility(View.GONE);
        }

        java.io.File outputDir = requireContext().getCacheDir();
        java.io.File outputFile;
        try {
            outputFile = java.io.File.createTempFile("trimmed_video", ".mp4", outputDir);
        } catch (java.io.IOException e) {
            if (layoutOptimizationOverlay != null) layoutOptimizationOverlay.setVisibility(View.GONE);
            Toast.makeText(getContext(), "Failed to create temp file", Toast.LENGTH_SHORT).show();
            return;
        }

        Transformer transformer = new Transformer.Builder(requireContext())
                .setVideoMimeType(MimeTypes.VIDEO_H264)
                .build();

        MediaItem mediaItem = new MediaItem.Builder()
                .setUri(inputUri)
                .setClippingConfiguration(
                        new MediaItem.ClippingConfiguration.Builder()
                                .setEndPositionMs(20000)
                                .build()
                )
                .build();

        transformer.addListener(new Transformer.Listener() {
            @Override
            public void onCompleted(Composition composition, ExportResult exportResult) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (layoutOptimizationOverlay != null) layoutOptimizationOverlay.setVisibility(View.GONE);
                        selectedVideoUri = Uri.fromFile(outputFile);
                        preparePreview(selectedVideoUri);
                        Toast.makeText(getContext(), "Trimmed to first 20s", Toast.LENGTH_SHORT).show();
                    });
                }
            }

            @Override
            public void onError(Composition composition, ExportResult exportResult, ExportException exportException) {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        if (layoutOptimizationOverlay != null) layoutOptimizationOverlay.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Cropping failed, using original", Toast.LENGTH_SHORT).show();
                        preparePreview(inputUri);
                    });
                }
            }
        });

        transformer.start(mediaItem, outputFile.getAbsolutePath());
    }

    private void preparePreview(Uri uri) {
        if (mPlayer != null) {
            mPlayer.release();
        }

        mPlayer = new ExoPlayer.Builder(requireContext()).build();
        mPlayer.setMediaItem(MediaItem.fromUri(uri));
        mPlayer.setRepeatMode(Player.REPEAT_MODE_ALL);
        mPlayer.prepare();
        mPlayer.play();

        videoPreview.setPlayer(mPlayer);
        videoPreview.setVisibility(View.VISIBLE);

        btnUpload.setEnabled(true);
        statusText.setText("Video ready");
    }

    private void uploadVideo() {
        if (selectedVideoUri == null) return;

        // Capture all input values on the UI thread
        String name = etName.getText().toString().trim();
        String mobile = etMobile.getText().toString().trim();
        String area = etArea.getText().toString().trim();
        String cost = etCost.getText().toString().trim();
        String caption = etCaption.getText().toString().trim();

        if (area.isEmpty()) {
            Toast.makeText(getContext(), "Area is mandatory", Toast.LENGTH_SHORT).show();
            return;
        }
        if (cost.isEmpty()) {
            Toast.makeText(getContext(), "Rate is mandatory", Toast.LENGTH_SHORT).show();
            return;
        }

        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) {
            startActivity(new Intent(getActivity(), LoginActivity.class));
            if (getActivity() != null) getActivity().finish();
            return;
        }

        btnUpload.setEnabled(false);
        if (layoutOptimizationOverlay != null) {
            layoutOptimizationOverlay.setVisibility(View.VISIBLE);
            if (tvOverlayText != null) tvOverlayText.setText("Uploading Coconut Video...");
            if (pbOverlayProgress != null) {
                pbOverlayProgress.setVisibility(View.VISIBLE);
                pbOverlayProgress.setProgress(0);
            }
        }
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
                        // Total bytes might be unknown initially
                        long effectiveTotal = totalBytes > 0 ? totalBytes : getFileSize(selectedVideoUri);
                        if (effectiveTotal <= 0) return;

                        final int progress = (int) ((bytes * 100L) / effectiveTotal);

                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (progressBar != null) progressBar.setProgress(progress);
                                if (pbOverlayProgress != null) {
                                    if (pbOverlayProgress.getVisibility() != View.VISIBLE) {
                                        pbOverlayProgress.setVisibility(View.VISIBLE);
                                    }
                                    pbOverlayProgress.setProgress(progress);
                                }
                            });
                        }
                    }

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        String pId = (String) resultData.get("public_id");
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (layoutOptimizationOverlay != null) layoutOptimizationOverlay.setVisibility(View.GONE);
                                saveToFirebase(url, pId, user.getUid(), name, mobile, area, cost, caption);
                            });
                        }
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        Log.e(TAG, "Cloudinary upload error: " + error.getDescription() + " code: " + error.getCode());
                        if (isAdded() && getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                if (layoutOptimizationOverlay != null) layoutOptimizationOverlay.setVisibility(View.GONE);
                                progressBar.setVisibility(View.GONE);
                                btnUpload.setEnabled(true);
                                Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show();
                            });
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveToFirebase(String url, String publicId, String userId, String name, String mobile, String area, String cost, String caption) {
        if (!isAdded() || mDatabase == null) return;

        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        if (currentUser != null) {
            Log.d(TAG, "Saving video metadata to Firebase. User UID: " + currentUser.getUid() + ", Email: " + currentUser.getEmail());
            if (currentUser.isAnonymous()) {
                Log.w(TAG, "User is authenticated ANONYMOUSLY. Ensure security rules allow this.");
            }
        } else {
            Log.e(TAG, "Attempting to save to Firebase but currentUser is NULL!");
        }

        String videoId = mDatabase.push().getKey();
        FirebaseUser user = mAuth.getCurrentUser();
        String profileName = (user != null && user.getDisplayName() != null) ? user.getDisplayName() : "";

        Map<String, Object> data = new HashMap<>();
        data.put("objectUrl", url);
        data.put("publicId", publicId);
        data.put("uploadedBy", userId);
        data.put("authorName", profileName);
        data.put("createdAt", System.currentTimeMillis());
        data.put("name", name);
        data.put("mobile", mobile);
        data.put("area", area);
        data.put("cost", cost);
        data.put("caption", caption);

        if (videoId != null) {
            mDatabase.child(videoId).setValue(data).addOnCompleteListener(task -> {
                if (isAdded() && getActivity() != null) {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Success!", Toast.LENGTH_SHORT).show();
                        // Navigate to feed
                        if (getActivity() instanceof MainActivity) {
                            ((MainActivity) getActivity()).replaceFragment(new FeedFragment());
                            // Update bottom nav selection
                            BottomNavigationView nav = getActivity().findViewById(R.id.bottom_navigation);
                            if (nav != null) nav.setSelectedItemId(R.id.nav_feed);
                        }
                    } else {
                        progressBar.setVisibility(View.GONE);
                        btnUpload.setEnabled(true);
                        String errorMsg = task.getException() != null ? task.getException().getMessage() : "Unknown error";
                        Log.e(TAG, "Firebase save failed: " + errorMsg);
                        Toast.makeText(getContext(), "Firebase save failed: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                }
            });
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mPlayer != null) {
            mPlayer.pause();
            mPlayer.setPlayWhenReady(false);
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
