package com.example.cocotrade;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
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
