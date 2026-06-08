package com.example.cocotrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
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
            Glide.with(holder.itemView.getContext())
                .load(post.objectUrl)
                .centerCrop()
                .into(holder.ivThumb);
        }
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
