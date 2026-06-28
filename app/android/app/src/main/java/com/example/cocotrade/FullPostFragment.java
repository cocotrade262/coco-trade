package com.example.cocotrade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.PagerSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class FullPostFragment extends Fragment {

    private List<VideoAdapter.VideoPost> videoPosts;
    private int initialPosition;

    public FullPostFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            videoPosts = (List<VideoAdapter.VideoPost>) getArguments().getSerializable("video_posts");
            initialPosition = getArguments().getInt("initial_position");
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_post, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(View.GONE);
        }

        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_full_post);
        LinearLayoutManager layoutManager = new LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        PagerSnapHelper snapHelper = new PagerSnapHelper();
        snapHelper.attachToRecyclerView(recyclerView);

        if (videoPosts == null) videoPosts = new ArrayList<>();
        VideoAdapter adapter = new VideoAdapter(videoPosts);
        recyclerView.setAdapter(adapter);
        recyclerView.scrollToPosition(initialPosition);

        view.findViewById(R.id.btn_back_full_post).setOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().getSupportFragmentManager().popBackStack();
            }
        });

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    playVisibleVideo(recyclerView);
                }
            }
        });

        recyclerView.postDelayed(() -> playVisibleVideo(recyclerView), 200);

        return view;
    }

    private void playVisibleVideo(RecyclerView recyclerView) {
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
        RecyclerView recyclerView = getView() != null ? getView().findViewById(R.id.recycler_view_full_post) : null;
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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setBottomNavVisibility(View.VISIBLE);
        }
    }
}
