package com.example.cocotrade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class FeedFragment extends Fragment {

    private DatabaseReference mDatabase;
    private RecyclerView recyclerView;
    private VideoAdapter adapter;
    private List<VideoAdapter.VideoPost> videoList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_feed, container, false);

        mDatabase = FirebaseDatabase.getInstance().getReference("UserVideos");
        recyclerView = view.findViewById(R.id.recycler_view_videos);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        videoList = new ArrayList<>();
        adapter = new VideoAdapter(videoList);
        recyclerView.setAdapter(adapter);

        loadVideos();
        return view;
    }

    private void loadVideos() {
        mDatabase.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                videoList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    VideoAdapter.VideoPost post = data.getValue(VideoAdapter.VideoPost.class);
                    if (post != null) {
                        post.id = data.getKey();
                        videoList.add(0, post);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load videos", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }
}
