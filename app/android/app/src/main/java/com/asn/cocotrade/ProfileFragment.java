package com.asn.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;
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
            String displayName = user.getDisplayName() != null && !user.getDisplayName().isEmpty() ? user.getDisplayName() : "Anonymous User";
            tvName.setText(displayName);
            tvEmail.setText(user.getEmail());

            // User specifically requested the first letter of the EMAIL to be displayed
            String initial = "";
            if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                initial = String.valueOf(user.getEmail().charAt(0)).toUpperCase();
            } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                initial = String.valueOf(user.getDisplayName().charAt(0)).toUpperCase();
            } else {
                initial = "U";
            }
            tvInitial.setText(initial);
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
        View btnTerms = view.findViewById(R.id.layout_btn_terms);
        View btnPrivacy = view.findViewById(R.id.layout_btn_privacy);
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

        btnTerms.setOnClickListener(v -> showTermsDialog());

        btnPrivacy.setOnClickListener(v -> showPrivacyDialog());

        btnSignOut.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "Logout successful", Toast.LENGTH_SHORT).show();
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

                        // If no custom profile image, try falling back to Firebase User profile picture (Google account)
                        if (currentProfileImageUrl == null || currentProfileImageUrl.isEmpty()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null && user.getPhotoUrl() != null) {
                                currentProfileImageUrl = user.getPhotoUrl().toString();
                            }
                        }

                        if (currentProfileImageUrl != null && !currentProfileImageUrl.isEmpty()) {
                            tvInitial.setVisibility(View.GONE);
                            ivProfilePhoto.setVisibility(View.VISIBLE);
                            Glide.with(ProfileFragment.this)
                                    .load(currentProfileImageUrl)
                                    .circleCrop()
                                    .into(ivProfilePhoto);
                        } else {
                            // Ensure initial is visible if no image
                            tvInitial.setVisibility(View.VISIBLE);
                            ivProfilePhoto.setVisibility(View.GONE);

                            // Re-apply initial logic to ensure it's up to date
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user != null) {
                                String initial = "U";
                                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                                    initial = String.valueOf(user.getEmail().charAt(0)).toUpperCase();
                                } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                                    initial = String.valueOf(user.getDisplayName().charAt(0)).toUpperCase();
                                }
                                tvInitial.setText(initial);
                            }
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
                                Toast.makeText(getContext(), "Upload failed: " + error.getDescription(), Toast.LENGTH_LONG).show());
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

    private void showTermsDialog() {
        String termsText = "COCOTRADE – TERMS AND CONDITIONS\n\n" +
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

        new AlertDialog.Builder(requireContext())
                .setTitle("Terms and Conditions")
                .setMessage(termsText)
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

        new AlertDialog.Builder(requireContext())
                .setTitle("Privacy Policy")
                .setMessage(privacyText)
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
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
            public void onCancelled(@NonNull DatabaseError error) {
                if (isAdded() && mAuth.getCurrentUser() != null) {
                    Log.e("ProfileFragment", "Load my videos failed: " + error.getMessage());
                    Toast.makeText(getContext(), "Failed to load your posts: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
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
