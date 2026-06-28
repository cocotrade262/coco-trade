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
    private ValueEventListener mValueEventListener;
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

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playVisibleVideo();
                }
            }
        });

        loadVideos();
        return view;
    }

    private void playVisibleVideo() {
        if (!isResumed()) return;
        LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
        if (layoutManager == null) return;

        int firstVisible = layoutManager.findFirstVisibleItemPosition();
        int lastVisible = layoutManager.findLastVisibleItemPosition();

        for (int i = firstVisible; i <= lastVisible; i++) {
            VideoAdapter.VideoViewHolder holder = (VideoAdapter.VideoViewHolder) recyclerView.findViewHolderForAdapterPosition(i);
            if (holder != null) {
                View itemView = holder.itemView;
                int[] location = new int[2];
                itemView.getLocationOnScreen(location);
                int viewTop = location[1];
                int viewBottom = viewTop + itemView.getHeight();
                int screenCenter = recyclerView.getContext().getResources().getDisplayMetrics().heightPixels / 2;

                if (viewTop <= screenCenter && viewBottom >= screenCenter) {
                    if (holder.mPlayer != null && !holder.mPlayer.isPlaying()) {
                        holder.mPlayer.play();
                    }
                } else {
                    if (holder.mPlayer != null && holder.mPlayer.isPlaying()) {
                        holder.mPlayer.pause();
                    }
                }
            }
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        pauseAllVideos();
    }

    private void pauseAllVideos() {
        if (recyclerView == null) return;
        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            RecyclerView.ViewHolder vh = recyclerView.getChildViewHolder(child);
            if (vh instanceof VideoAdapter.VideoViewHolder) {
                VideoAdapter.VideoViewHolder holder = (VideoAdapter.VideoViewHolder) vh;
                if (holder.mPlayer != null) {
                    holder.mPlayer.pause();
                    holder.mPlayer.setPlayWhenReady(false);
                }
            }
        }
    }

    private void loadVideos() {
        mValueEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;
                videoList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    VideoAdapter.VideoPost post = data.getValue(VideoAdapter.VideoPost.class);
                    if (post != null && !post.isSold) {
                        post.id = data.getKey();
                        videoList.add(0, post);
                    }
                }
                adapter.notifyDataSetChanged();
                recyclerView.postDelayed(() -> {
                    if (isAdded() && isResumed()) playVisibleVideo();
                }, 500);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Failed to load videos: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        };
        mDatabase.addValueEventListener(mValueEventListener);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mDatabase != null && mValueEventListener != null) {
            mDatabase.removeEventListener(mValueEventListener);
        }
    }
}
