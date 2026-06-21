package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.bumptech.glide.Glide;
import com.cloudinary.android.MediaManager;
import com.cloudinary.android.callback.ErrorInfo;
import com.cloudinary.android.callback.UploadCallback;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private Query mMyVideosQuery;
    private ValueEventListener mValueEventListener;
    private RecyclerView recyclerView;
    private VideoGridAdapter adapter;
    private List<VideoAdapter.VideoPost> myVideos;
    private TextView tvPostCount;
    private ImageView ivProfilePhoto, ivProfilePreviewLarge;
    private TextView tvInitial;
    private View layoutProfilePreview;
    private String currentProfileImageUrl;

    private final ActivityResultLauncher<String> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    uploadProfilePicture(uri);
                }
            }
    );

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("UserVideos");
        FirebaseUser user = mAuth.getCurrentUser();

        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvEmail = view.findViewById(R.id.tv_profile_email);
        tvInitial = view.findViewById(R.id.tv_profile_initial);
        ivProfilePhoto = view.findViewById(R.id.iv_profile_photo);
        tvPostCount = view.findViewById(R.id.tv_post_count);

        ivProfilePreviewLarge = view.findViewById(R.id.iv_profile_preview_large);
        layoutProfilePreview = view.findViewById(R.id.layout_profile_preview);
        View btnClosePreview = view.findViewById(R.id.btn_close_preview);

        if (user != null) {
            String displayName = user.getDisplayName() != null ? user.getDisplayName() : "Anonymous User";
            tvName.setText(displayName);
            tvEmail.setText(user.getEmail());

            if (!displayName.isEmpty()) {
                tvInitial.setText(String.valueOf(displayName.charAt(0)).toUpperCase());
            }
        }

        recyclerView = view.findViewById(R.id.recycler_view_my_videos);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 3));
        myVideos = new ArrayList<>();
        adapter = new VideoGridAdapter(myVideos);
        recyclerView.setAdapter(adapter);

        View tabPosts = view.findViewById(R.id.tab_my_posts);
        View tabSettings = view.findViewById(R.id.tab_settings);
        View tabReport = view.findViewById(R.id.tab_report);

        ImageView ivPosts = view.findViewById(R.id.iv_tab_posts);
        TextView tvPosts = view.findViewById(R.id.tv_tab_posts);
        ImageView ivSettings = view.findViewById(R.id.iv_tab_settings);
        TextView tvSettings = view.findViewById(R.id.tv_tab_settings);
        ImageView ivReport = view.findViewById(R.id.iv_tab_report);
        TextView tvReport = view.findViewById(R.id.tv_tab_report);

        View layoutSettings = view.findViewById(R.id.layout_settings_content);
        View btnChangePhoto = view.findViewById(R.id.layout_btn_change_photo);
        View btnSignOut = view.findViewById(R.id.layout_btn_sign_out);

        View layoutReport = view.findViewById(R.id.layout_report_content);
        Spinner spinnerReport = view.findViewById(R.id.spinner_report_type);
        EditText etReportMessage = view.findViewById(R.id.et_report_message);
        Button btnSubmitFeedback = view.findViewById(R.id.btn_submit_feedback);

        String[] reportTypes = {"Report", "Suggestion", "Other"};
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, reportTypes);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerReport.setAdapter(spinnerAdapter);

        tabPosts.setOnClickListener(v -> {
            ivPosts.setColorFilter(0xFF3880FF);
            tvPosts.setTextColor(0xFF3880FF);
            ivSettings.setColorFilter(0xFFFFFFFF);
            tvSettings.setTextColor(0xFFFFFFFF);
            ivReport.setColorFilter(0xFFFFFFFF);
            tvReport.setTextColor(0xFFFFFFFF);

            recyclerView.setVisibility(View.VISIBLE);
            layoutSettings.setVisibility(View.GONE);
            layoutReport.setVisibility(View.GONE);
        });

        tabSettings.setOnClickListener(v -> {
            ivPosts.setColorFilter(0xFFFFFFFF);
            tvPosts.setTextColor(0xFFFFFFFF);
            ivSettings.setColorFilter(0xFF3880FF);
            tvSettings.setTextColor(0xFF3880FF);
            ivReport.setColorFilter(0xFFFFFFFF);
            tvReport.setTextColor(0xFFFFFFFF);

            recyclerView.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.VISIBLE);
            layoutReport.setVisibility(View.GONE);
        });

        btnChangePhoto.setOnClickListener(v -> pickImageLauncher.launch("image/*"));

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            if (getActivity() != null) getActivity().finish();
        });

        tabReport.setOnClickListener(v -> {
            ivPosts.setColorFilter(0xFFFFFFFF);
            tvPosts.setTextColor(0xFFFFFFFF);
            ivSettings.setColorFilter(0xFFFFFFFF);
            tvSettings.setTextColor(0xFFFFFFFF);
            ivReport.setColorFilter(0xFF3880FF);
            tvReport.setTextColor(0xFF3880FF);

            recyclerView.setVisibility(View.GONE);
            layoutSettings.setVisibility(View.GONE);
            layoutReport.setVisibility(View.VISIBLE);
        });

        view.findViewById(R.id.layout_profile_image).setOnClickListener(v -> {
            if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                Glide.with(this).load(currentProfileImageUrl).into(ivProfilePreviewLarge);
                layoutProfilePreview.setVisibility(View.VISIBLE);
            }
        });

        btnClosePreview.setOnClickListener(v -> layoutProfilePreview.setVisibility(View.GONE));
        layoutProfilePreview.setOnClickListener(v -> layoutProfilePreview.setVisibility(View.GONE));

        btnSubmitFeedback.setOnClickListener(v -> {
            String type = spinnerReport.getSelectedItem().toString();
            String message = etReportMessage.getText().toString().trim();

            if (message.isEmpty()) {
                Toast.makeText(requireContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:"));
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"cocotrade262@gmail.com"});
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, type + " from " + (user != null ? user.getEmail() : "Anonymous"));
            intent.putExtra(android.content.Intent.EXTRA_TEXT, message);
            startActivity(android.content.Intent.createChooser(intent, "Send Feedback..."));
        });

        if (user != null) {
            loadMyVideos(user.getUid());
            loadUserProfile(user.getUid());
        }

        return view;
    }

    private void loadUserProfile(String userId) {
        FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        if (!isAdded()) return;
                        currentProfileImageUrl = snapshot.child("profileImageUrl").getValue(String.class);
                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            tvInitial.setVisibility(View.GONE);
                            ivProfilePhoto.setVisibility(View.VISIBLE);
                            Glide.with(ProfileFragment.this)
                                    .load(currentProfileImageUrl)
                                    .circleCrop()
                                    .into(ivProfilePhoto);
                        } else {
                            tvInitial.setVisibility(View.VISIBLE);
                            ivProfilePhoto.setVisibility(View.GONE);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
    }

    private void uploadProfilePicture(Uri imageUri) {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user == null) return;

        Toast.makeText(getContext(), "Uploading profile picture...", Toast.LENGTH_SHORT).show();

        MediaManager.get().upload(imageUri)
                .unsigned("ml_default")
                .option("resource_type", "image")
                .callback(new UploadCallback() {
                    @Override
                    public void onStart(String requestId) {}

                    @Override
                    public void onProgress(String requestId, long bytes, long totalBytes) {}

                    @Override
                    public void onSuccess(String requestId, Map resultData) {
                        String url = (String) resultData.get("secure_url");
                        saveProfileUrlToFirebase(url, user.getUid());
                    }

                    @Override
                    public void onError(String requestId, ErrorInfo error) {
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() ->
                                Toast.makeText(getContext(), "Upload failed", Toast.LENGTH_SHORT).show());
                        }
                    }

                    @Override
                    public void onReschedule(String requestId, ErrorInfo error) {}
                })
                .dispatch();
    }

    private void saveProfileUrlToFirebase(String url, String userId) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImageUrl", url);

        FirebaseDatabase.getInstance().getReference("Users").child(userId)
                .updateChildren(updates)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Profile picture updated", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to update profile", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void loadMyVideos(String userId) {
        mMyVideosQuery = mDatabase.orderByChild("uploadedBy").equalTo(userId);
        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                myVideos.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    VideoAdapter.VideoPost post = data.getValue(VideoAdapter.VideoPost.class);
                    if (post != null && !post.isSold) {
                        post.id = data.getKey();
                        myVideos.add(0, post);
                    }
                }
                adapter.notifyDataSetChanged();
                tvPostCount.setText(myVideos.size() + " posts");
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        mMyVideosQuery.addValueEventListener(mValueEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mMyVideosQuery != null && mValueEventListener != null) {
            mMyVideosQuery.removeEventListener(mValueEventListener);
        }
    }
}
