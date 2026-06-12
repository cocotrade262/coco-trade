package com.example.cocotrade;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
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

        tabPosts.setOnClickListener(v -> {
            ivPosts.setColorFilter(0xFF3880FF);
            tvPosts.setTextColor(0xFF3880FF);
            ivSettings.setColorFilter(0xFFFFFFFF);
            tvSettings.setTextColor(0xFFFFFFFF);
            ivReport.setColorFilter(0xFFFFFFFF);
            tvReport.setTextColor(0xFFFFFFFF);
            recyclerView.setVisibility(View.VISIBLE);
        });

        tabSettings.setOnClickListener(v -> {
            ivPosts.setColorFilter(0xFFFFFFFF);
            tvPosts.setTextColor(0xFFFFFFFF);
            ivSettings.setColorFilter(0xFF3880FF);
            tvSettings.setTextColor(0xFF3880FF);
            ivReport.setColorFilter(0xFFFFFFFF);
            tvReport.setTextColor(0xFFFFFFFF);

            // Sign out for now as specific settings aren't defined
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

            Intent intent = new Intent(android.content.Intent.ACTION_SENDTO);
            intent.setData(android.net.Uri.parse("mailto:"));
            intent.putExtra(android.content.Intent.EXTRA_EMAIL, new String[]{"cocotrade262@gmail.com"});
            intent.putExtra(android.content.Intent.EXTRA_SUBJECT, "Report / Suggestion from " + (user != null ? user.getEmail() : "Anonymous"));
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
