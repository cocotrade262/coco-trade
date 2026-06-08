package com.example.cocotrade;

import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class VideoAdapter extends RecyclerView.Adapter<VideoAdapter.VideoViewHolder> {

    private List<VideoPost> videoPosts;
    private static boolean isMuted = false;

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
        holder.tvCaption.setText(post.caption != null ? post.caption : "");
        holder.tvCaption.setVisibility((post.caption != null && !post.caption.isEmpty()) ? View.VISIBLE : View.GONE);

        if (post.createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(post.createdAt)));
        }

        String currentUserId = FirebaseAuth.getInstance().getUid();
        boolean isOwner = post.uploadedBy != null && post.uploadedBy.equals(currentUserId);

        if (post.isSold) {
            holder.tvSoldLabel.setVisibility(View.VISIBLE);
            holder.btnMarkSold.setVisibility(View.GONE);
        } else {
            holder.tvSoldLabel.setVisibility(View.GONE);
            holder.btnMarkSold.setVisibility(isOwner ? View.VISIBLE : View.GONE);
        }

        holder.btnMarkSold.setOnClickListener(v -> {
            if (post.id != null) {
                FirebaseDatabase.getInstance().getReference("UserVideos")
                        .child(post.id).child("isSold").setValue(true);
            }
        });

        holder.layoutContact.setOnClickListener(v -> {
            if (post.mobile != null && !post.mobile.isEmpty()) {
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.setData(Uri.parse("tel:" + post.mobile));
                v.getContext().startActivity(intent);
            }
        });

        holder.layoutShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Check out this coconut trade: " + post.objectUrl);
            v.getContext().startActivity(Intent.createChooser(intent, "Share via"));
        });

        holder.layoutComment.setOnClickListener(v -> {
            Toast.makeText(v.getContext(), "Comments coming soon!", Toast.LENGTH_SHORT).show();
        });

        holder.btnReport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"cocotrade262@gmail.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Report Video: " + post.id);
            intent.putExtra(Intent.EXTRA_TEXT, "I would like to report this video for inappropriate content.");
            v.getContext().startActivity(Intent.createChooser(intent, "Send Report..."));
        });

        if (post.objectUrl != null) {
            String tag = (String) holder.videoView.getTag();
            if (tag == null || !tag.equals(post.objectUrl)) {
                holder.videoView.setVideoPath(post.objectUrl);
                holder.videoView.setTag(post.objectUrl);
                holder.videoView.setOnPreparedListener(mp -> {
                    mp.setLooping(true);
                    holder.mPlayer = mp;
                    applyMuteState(mp);
                    // Do not auto-start here, FeedFragment will manage playback
                });
            } else if (holder.mPlayer != null) {
                applyMuteState(holder.mPlayer);
            }
        }

        holder.itemView.setOnClickListener(v -> {
            isMuted = !isMuted;
            notifyDataSetChanged();
            showMuteIcon(holder.ivMuteToggle);
        });
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.videoView != null) {
            holder.videoView.stopPlayback();
            holder.videoView.setTag(null);
        }
    }

    private void applyMuteState(MediaPlayer mp) {
        if (isMuted) {
            mp.setVolume(0, 0);
        } else {
            mp.setVolume(1, 1);
        }
    }

    private void showMuteIcon(ImageView iv) {
        iv.setImageResource(isMuted ? android.R.drawable.ic_lock_silent_mode : android.R.drawable.ic_lock_silent_mode_off);
        iv.setAlpha(1.0f);
        iv.animate().alpha(0f).setDuration(1000).start();
    }

    @Override
    public int getItemCount() {
        return videoPosts.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        VideoView videoView;
        TextView tvName, tvArea, tvCost, tvCaption, tvSoldLabel, tvDate;
        Button btnMarkSold;
        LinearLayout layoutContact, layoutShare, layoutComment;
        ImageButton btnReport;
        ImageView ivMuteToggle;
        MediaPlayer mPlayer;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            videoView = itemView.findViewById(R.id.video_view_item);
            tvName = itemView.findViewById(R.id.tv_item_name);
            tvArea = itemView.findViewById(R.id.tv_item_area);
            tvCost = itemView.findViewById(R.id.tv_item_cost);
            tvCaption = itemView.findViewById(R.id.tv_item_caption);
            tvSoldLabel = itemView.findViewById(R.id.tv_sold_label);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            btnMarkSold = itemView.findViewById(R.id.btn_mark_sold);
            layoutContact = itemView.findViewById(R.id.btn_item_contact_layout);
            layoutShare = itemView.findViewById(R.id.btn_item_share_layout);
            layoutComment = itemView.findViewById(R.id.btn_item_comment_layout);
            btnReport = itemView.findViewById(R.id.btn_report);
            ivMuteToggle = itemView.findViewById(R.id.iv_mute_toggle);
        }
    }

    public static class VideoPost {
        public String id;
        public String objectUrl;
        public String name;
        public String area;
        public String cost;
        public String caption;
        public String mobile;
        public String uploadedBy;
        public long createdAt;
        public boolean isSold;

        public VideoPost() {} // Required for Firebase
    }
}
