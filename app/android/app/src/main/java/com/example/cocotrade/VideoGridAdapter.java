package com.example.cocotrade;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class VideoGridAdapter extends RecyclerView.Adapter<VideoGridAdapter.GridViewHolder> {

    private List<VideoAdapter.VideoPost> videoPosts;

    public VideoGridAdapter(List<VideoAdapter.VideoPost> videoPosts) {
        this.videoPosts = videoPosts;
    }

    @NonNull
    @Override
    public GridViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video_grid, parent, false);
        return new GridViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GridViewHolder holder, int position) {
        VideoAdapter.VideoPost post = videoPosts.get(position);
        if (post.objectUrl != null) {
            String optimizedUrl = post.objectUrl;
            if (optimizedUrl.contains("/upload/")) {
                optimizedUrl = optimizedUrl.replace("/upload/", "/upload/f_auto,q_auto,w_240,vc_h264/");
            }
            Glide.with(holder.itemView.getContext())
                .load(optimizedUrl)
                .centerCrop()
                .into(holder.ivThumb);
        }

        holder.itemView.setOnClickListener(v -> {
            if (v.getContext() instanceof MainActivity) {
                FullPostFragment fragment = new FullPostFragment();
                Bundle args = new Bundle();
                args.putSerializable("video_posts", new ArrayList<>(videoPosts));
                args.putInt("initial_position", position);
                fragment.setArguments(args);
                ((MainActivity) v.getContext()).replaceFragment(fragment, true);
            }
        });
    }

    @Override
    public int getItemCount() {
        return videoPosts.size();
    }

    public static class GridViewHolder extends RecyclerView.ViewHolder {
        ImageView ivThumb;

        public GridViewHolder(@NonNull View itemView) {
            super(itemView);
            ivThumb = itemView.findViewById(R.id.iv_grid_thumb);
        }
    }
}
