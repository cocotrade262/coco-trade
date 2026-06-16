package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import android.graphics.Outline;
import android.view.ViewOutlineProvider;
import androidx.annotation.NonNull;
import androidx.annotation.OptIn;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
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
        String profileName = post.authorName != null ? post.authorName : post.name;
        holder.tvName.setText(profileName != null ? "@" + profileName : "Coconut Seller");
        holder.tvArea.setText(post.area != null ? post.area : "Unknown Area");
        holder.tvCost.setText(post.cost != null ? "₹" + post.cost : "");
        holder.tvCaption.setText(post.caption != null ? post.caption : "");
        holder.tvCaption.setVisibility((post.caption != null && !post.caption.isEmpty()) ? View.VISIBLE : View.GONE);

        if (post.createdAt > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            holder.tvDate.setText(sdf.format(new Date(post.createdAt)));
        }

        if (post.name != null && !post.name.isEmpty()) {
            holder.tvInitial.setText(String.valueOf(post.name.charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("C");
        }

        if (post.isSold) {
            holder.tvSoldLabel.setVisibility(View.VISIBLE);
        } else {
            holder.tvSoldLabel.setVisibility(View.GONE);
        }

        holder.tvDetailName.setText("Seller: " + (post.name != null ? post.name : "N/A"));
        holder.tvDetailMobile.setText("Call: " + (post.mobile != null ? post.mobile : "N/A"));

        holder.layoutContact.setOnClickListener(v -> {
            if (holder.layoutContactDetails.getVisibility() == View.VISIBLE) {
                holder.layoutContactDetails.setVisibility(View.GONE);
            } else {
                holder.layoutContactDetails.setVisibility(View.VISIBLE);
            }
        });

        holder.tvDetailMobile.setOnClickListener(v -> {
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

        String currentUserUid = FirebaseAuth.getInstance().getUid();
        if (currentUserUid != null && currentUserUid.equals(post.uploadedBy) && !post.isSold) {
            holder.ivMoreOptions.setVisibility(View.VISIBLE);
            holder.ivMoreOptions.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), v);
                popup.getMenu().add("Mark as Sold");
                popup.setOnMenuItemClickListener(item -> {
                    if (item.getTitle().equals("Mark as Sold")) {
                        new AlertDialog.Builder(v.getContext())
                                .setTitle("Mark as Sold")
                                .setMessage("Are you sure you want to mark this as sold? It will be removed from the feed.")
                                .setPositiveButton("Yes", (dialog, which) -> {
                                    FirebaseDatabase.getInstance().getReference("UserVideos")
                                            .child(post.id)
                                            .child("isSold")
                                            .setValue(true)
                                            .addOnSuccessListener(aVoid -> {
                                                Toast.makeText(v.getContext(), "Marked as sold", Toast.LENGTH_SHORT).show();
                                            });
                                })
                                .setNegativeButton("No", null)
                                .show();
                        return true;
                    }
                    return false;
                });
                popup.show();
            });
        } else {
            holder.ivMoreOptions.setVisibility(View.GONE);
        }

        if (post.objectUrl != null) {
            initializePlayer(holder, post.objectUrl);
        }

        holder.ivMuteToggle.setAlpha(0f);

        holder.itemView.setOnClickListener(v -> {
            isMuted = !isMuted;
            applyMuteToAllVisibleHolders(v);
            showMuteIcon(holder.ivMuteToggle);
        });
    }

    private void applyMuteToAllVisibleHolders(View view) {
        RecyclerView rv = null;
        View parent = (View) view.getParent();
        while (parent != null && !(parent instanceof RecyclerView)) {
            parent = (View) parent.getParent();
        }
        if (parent instanceof RecyclerView) {
            rv = (RecyclerView) parent;
            for (int i = 0; i < rv.getChildCount(); i++) {
                View child = rv.getChildAt(i);
                VideoViewHolder vh = (VideoViewHolder) rv.getChildViewHolder(child);
                if (vh != null && vh.mPlayer != null) {
                    applyMuteState(vh.mPlayer);
                }
            }
        }
    }

    private void initializePlayer(VideoViewHolder holder, String videoUrl) {
        String tag = (String) holder.playerView.getTag();
        if (tag == null || !tag.equals(videoUrl) || holder.mPlayer == null) {
            if (holder.mPlayer != null) {
                holder.mPlayer.release();
            }
            ExoPlayer player = new ExoPlayer.Builder(holder.itemView.getContext()).build();
            player.setMediaItem(MediaItem.fromUri(videoUrl));
            player.setRepeatMode(Player.REPEAT_MODE_ALL);
            player.prepare();
            holder.playerView.setPlayer(player);
            holder.playerView.setTag(videoUrl);
            holder.mPlayer = player;
            applyMuteState(player);
        } else {
            applyMuteState(holder.mPlayer);
        }
    }

    @Override
    public void onViewAttachedToWindow(@NonNull VideoViewHolder holder) {
        super.onViewAttachedToWindow(holder);
        int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION && position < videoPosts.size()) {
            VideoPost post = videoPosts.get(position);
            if (post.objectUrl != null) {
                initializePlayer(holder, post.objectUrl);
            }
        }
    }

    @Override
    public void onViewDetachedFromWindow(@NonNull VideoViewHolder holder) {
        super.onViewDetachedFromWindow(holder);
        if (holder.mPlayer != null) {
            holder.mPlayer.release();
            holder.mPlayer = null;
            holder.playerView.setPlayer(null);
            // We keep the tag so we know what was last loaded,
            // but initializePlayer will re-create if mPlayer is null
        }
    }

    private void applyMuteState(ExoPlayer mp) {
        if (isMuted) {
            mp.setVolume(0f);
        } else {
            mp.setVolume(1f);
        }
    }

    private void showMuteIcon(ImageView iv) {
        iv.setImageResource(isMuted ? R.drawable.ic_mute_outline : R.drawable.ic_unmute_outline);
        iv.setAlpha(1.0f);
        iv.animate().alpha(0f).setStartDelay(500).setDuration(1000).start();
    }

    @Override
    public int getItemCount() {
        return videoPosts.size();
    }

    public static class VideoViewHolder extends RecyclerView.ViewHolder {
        PlayerView playerView;
        TextView tvName, tvArea, tvCost, tvCaption, tvSoldLabel, tvDate, tvInitial;
        TextView tvDetailName, tvDetailMobile;
        LinearLayout layoutContact, layoutShare, layoutComment, layoutContactDetails;
        ImageView ivMuteToggle, ivMoreOptions;
        ExoPlayer mPlayer;
        ProgressBar progressBar;

        public VideoViewHolder(@NonNull View itemView) {
            super(itemView);
            playerView = itemView.findViewById(R.id.video_view_item);
            progressBar = itemView.findViewById(R.id.video_progress);

            // Apply outline provider for rounded corners on PlayerView
            playerView.setOutlineProvider(new ViewOutlineProvider() {
                @Override
                public void getOutline(View view, Outline outline) {
                    float radius = view.getContext().getResources().getDisplayMetrics().density * 24;
                    outline.setRoundRect(0, 0, view.getWidth(), view.getHeight(), radius);
                }
            });
            playerView.setClipToOutline(true);

            tvName = itemView.findViewById(R.id.tv_item_name);
            tvArea = itemView.findViewById(R.id.tv_item_area);
            tvCost = itemView.findViewById(R.id.tv_item_cost);
            tvCaption = itemView.findViewById(R.id.tv_item_caption);
            tvSoldLabel = itemView.findViewById(R.id.tv_sold_label);
            tvDate = itemView.findViewById(R.id.tv_item_date);
            tvInitial = itemView.findViewById(R.id.tv_item_initial);
            tvDetailName = itemView.findViewById(R.id.tv_detail_name);
            tvDetailMobile = itemView.findViewById(R.id.tv_detail_mobile);
            layoutContact = itemView.findViewById(R.id.btn_item_contact_layout);
            layoutShare = itemView.findViewById(R.id.btn_item_share_layout);
            layoutComment = itemView.findViewById(R.id.btn_item_comment_layout);
            ivMoreOptions = itemView.findViewById(R.id.iv_more_options);
            layoutContactDetails = itemView.findViewById(R.id.layout_contact_details);
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
        public String authorName;
        public String uploadedBy;
        public long createdAt;
        public boolean isSold;

        public VideoPost() {} // Required for Firebase
    }
}
