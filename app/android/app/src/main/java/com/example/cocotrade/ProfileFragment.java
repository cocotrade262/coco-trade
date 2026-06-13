package com.example.cocotrade;

import android.content.Intent;
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
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FirebaseAuth mAuth;
    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private VideoGridAdapter adapter;
    private List<VideoAdapter.VideoPost> myVideos;
    private TextView tvPostCount;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_profile, container, false);

        mAuth = FirebaseAuth.getInstance();
        mDatabase = FirebaseDatabase.getInstance().getReference("UserVideos");
        FirebaseUser user = mAuth.getCurrentUser();

        TextView tvName = view.findViewById(R.id.tv_profile_name);
        TextView tvEmail = view.findViewById(R.id.tv_profile_email);
        TextView tvInitial = view.findViewById(R.id.tv_profile_initial);
        tvPostCount = view.findViewById(R.id.tv_post_count);

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
        TextView btnSignOut = view.findViewById(R.id.tv_btn_sign_out);

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
        }

        return view;
    }

    private void loadMyVideos(String userId) {
        Query query = mDatabase.orderByChild("uploadedBy").equalTo(userId);
        query.addValueEventListener(new ValueEventListener() {
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
        });
    }
}
