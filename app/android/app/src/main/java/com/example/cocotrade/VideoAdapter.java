package com.example.cocotrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<VideoPost> videoPosts;

    public VideoAdapter(List<VideoPost> videoPosts) {
        this.videoPosts = videoPosts;
    }

    @NonNull
    @Override
    public VideoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_video, parent, false);
        return new VideoViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VideoViewHolder holder, int position) {
        VideoPost post = videoPosts.get(position);
        holder.tvName.setText(post.name != null ? post.name : "Coconut Seller");
        holder.tvArea.setText(post.area != null ? post.area : "Unknown Area");
        holder.tvCost.setText(post.cost != null ? "₹" + post.cost : "");
        holder.tvCaption.setText(post.caption);

        if (post.objectUrl != null) {
            holder.videoView.setVideoPath(post.objectUrl);
            holder.videoView.setOnPreparedListener(mp -> {
                mp.setLooping(true);
                holder.videoView.start();
            });
        }
    }

    @Override
    public int getItemCount() {
        return videoPosts.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        TextView tvName, tvArea, tvCost, tvCaption;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_view_item);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvArea = itemView.findViewById(R.id.tv_item_area);
            tvCost = itemView.findViewById(R.id.tv_item_cost);
            tvCaption = itemView.findViewById(R.id.tv_item_caption);
        }
    }

    public static class VideoPost {
        public String id;
        public String objectUrl;
        public String name;
        public String area;
        public String cost;
        public String caption;
        public String uploadedBy;
        public long createdAt;

        public VideoPost() {} // Required for Firebase
    }
}
