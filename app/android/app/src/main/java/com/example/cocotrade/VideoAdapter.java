package com.example.cocotrade;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.PopupMenu;
import android.widget.EditText;
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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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

        holder.layoutContact.setOnClickListener(v -> showDetailsBottomSheet(v, post));

        holder.layoutShare.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "Check out this coconut trade: " + post.objectUrl);
            v.getContext().startActivity(Intent.createChooser(intent, "Share via"));
        });

        holder.layoutComment.setOnClickListener(v -> showCommentsBottomSheet(v, post));

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

    private void showDetailsBottomSheet(View v, VideoPost post) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext());
        View view = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_details_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        EditText etCaption = view.findViewById(R.id.et_details_caption);
        EditText etName = view.findViewById(R.id.et_details_name);
        EditText etMobile = view.findViewById(R.id.et_details_mobile);
        EditText etArea = view.findViewById(R.id.et_details_area);
        EditText etCost = view.findViewById(R.id.et_details_cost);
        Button btnUpdate = view.findViewById(R.id.btn_update_details);
        Button btnCall = view.findViewById(R.id.btn_call_seller);

        etCaption.setText(post.caption);
        etName.setText(post.name);
        etMobile.setText(post.mobile);
        etArea.setText(post.area);
        etCost.setText(post.cost);

        String currentUserId = FirebaseAuth.getInstance().getUid();
        boolean isOwner = currentUserId != null && currentUserId.equals(post.uploadedBy);

        if (isOwner) {
            etCaption.setEnabled(true);
            etName.setEnabled(true);
            etMobile.setEnabled(true);
            etArea.setEnabled(true);
            etCost.setEnabled(true);
            btnUpdate.setVisibility(View.VISIBLE);
            btnCall.setVisibility(View.GONE);
            btnUpdate.setOnClickListener(btnV -> {
                DatabaseReference ref = FirebaseDatabase.getInstance().getReference("UserVideos").child(post.id);
                ref.child("caption").setValue(etCaption.getText().toString().trim());
                ref.child("name").setValue(etName.getText().toString().trim());
                ref.child("mobile").setValue(etMobile.getText().toString().trim());
                ref.child("area").setValue(etArea.getText().toString().trim());
                ref.child("cost").setValue(etCost.getText().toString().trim())
                        .addOnSuccessListener(aVoid -> {
                            Toast.makeText(v.getContext(), "Updated successfully", Toast.LENGTH_SHORT).show();
                            bottomSheetDialog.dismiss();
                        });
            });
        } else {
            etCaption.setEnabled(false);
            etName.setEnabled(false);
            etMobile.setEnabled(false);
            etArea.setEnabled(false);
            etCost.setEnabled(false);
            btnUpdate.setVisibility(View.GONE);
            btnCall.setVisibility(View.VISIBLE);
            btnCall.setOnClickListener(btnV -> {
                if (post.mobile != null && !post.mobile.isEmpty()) {
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + post.mobile));
                    v.getContext().startActivity(intent);
                }
            });
        }

        bottomSheetDialog.show();
    }

    private void showDMBottomSheet(View v, VideoPost post) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext());
        View view = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_dm_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        TextView tvRecipient = view.findViewById(R.id.tv_dm_recipient);
        EditText etMessage = view.findViewById(R.id.et_dm_message);
        Button btnSend = view.findViewById(R.id.btn_send_dm);

        String profileName = post.authorName != null ? post.authorName : post.name;
        tvRecipient.setText("To: @" + (profileName != null ? profileName : "Seller"));

        btnSend.setOnClickListener(btnV -> {
            String message = etMessage.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(v.getContext(), "Please enter a message", Toast.LENGTH_SHORT).show();
                return;
            }

            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) return;

            Map<String, Object> dmData = new HashMap<>();
            dmData.put("fromId", currentUser.getUid());
            dmData.put("fromName", currentUser.getDisplayName());
            dmData.put("toId", post.uploadedBy);
            dmData.put("postId", post.id);
            dmData.put("message", message);
            dmData.put("timestamp", System.currentTimeMillis());

            FirebaseDatabase.getInstance().getReference("DirectMessages")
                    .push()
                    .setValue(dmData)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(v.getContext(), "Message sent successfully!", Toast.LENGTH_SHORT).show();
                        bottomSheetDialog.dismiss();
                    });
        });

        bottomSheetDialog.show();
    }

    private void showCommentsBottomSheet(View v, VideoPost post) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(v.getContext());
        View view = LayoutInflater.from(v.getContext()).inflate(R.layout.layout_comments_bottom_sheet, null);
        bottomSheetDialog.setContentView(view);

        RecyclerView rvComments = view.findViewById(R.id.recycler_view_comments);
        EditText etInput = view.findViewById(R.id.et_comment_input);
        TextView tvPost = view.findViewById(R.id.tv_post_comment);

        java.util.List<CommentAdapter.Comment> comments = new java.util.ArrayList<>();
        CommentAdapter adapter = new CommentAdapter(comments, post.id, comment -> {
            etInput.setText("@" + comment.userName + " ");
            etInput.setTag(comment);
            Toast.makeText(v.getContext(), "Replying to " + comment.userName, Toast.LENGTH_SHORT).show();
        });
        rvComments.setLayoutManager(new LinearLayoutManager(v.getContext()));
        rvComments.setAdapter(adapter);

        DatabaseReference commentsRef = FirebaseDatabase.getInstance().getReference("Comments").child(post.id);
        ValueEventListener commentsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                java.util.List<CommentAdapter.Comment> allComments = new java.util.ArrayList<>();
                String currentUserId = FirebaseAuth.getInstance().getUid();

                for (DataSnapshot data : snapshot.getChildren()) {
                    CommentAdapter.Comment c = data.getValue(CommentAdapter.Comment.class);
                    if (c != null) {
                        c.id = data.getKey();
                        // Filter private comments here: visible only to sender or intended recipient
                        boolean isVisible = !c.isPrivate ||
                                (currentUserId != null && (currentUserId.equals(c.userId) || currentUserId.equals(c.replyToUserId)));
                        if (isVisible) {
                            allComments.add(c);
                        }
                    }
                }

                comments.clear();
                // Organize into threads efficiently
                java.util.Map<String, java.util.List<CommentAdapter.Comment>> repliesMap = new java.util.HashMap<>();
                java.util.List<CommentAdapter.Comment> parents = new java.util.ArrayList<>();

                for (CommentAdapter.Comment c : allComments) {
                    if (c.parentCommentId == null) {
                        parents.add(c);
                    } else {
                        if (!repliesMap.containsKey(c.parentCommentId)) {
                            repliesMap.put(c.parentCommentId, new java.util.ArrayList<>());
                        }
                        repliesMap.get(c.parentCommentId).add(c);
                    }
                }

                for (CommentAdapter.Comment p : parents) {
                    comments.add(p);
                    java.util.List<CommentAdapter.Comment> replies = repliesMap.get(p.id);
                    if (replies != null) {
                        comments.addAll(replies);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        };
        commentsRef.addValueEventListener(commentsListener);
        bottomSheetDialog.setOnDismissListener(dialog -> commentsRef.removeEventListener(commentsListener));

        tvPost.setOnClickListener(btnV -> {
            String text = etInput.getText().toString().trim();
            if (text.isEmpty()) return;

            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
            if (user == null) return;

            String userName = user.getDisplayName() != null ? user.getDisplayName() : "User";

            boolean isPrivate = false;
            String replyToUserId = null;
            CommentAdapter.Comment replyTo = (CommentAdapter.Comment) etInput.getTag();
            if (replyTo != null) {
                isPrivate = true;
                replyToUserId = replyTo.userId;
            }

            String parentCommentId = null;
            if (replyTo != null) {
                parentCommentId = replyTo.parentCommentId != null ? replyTo.parentCommentId : replyTo.id;
            }

            CommentAdapter.Comment comment = new CommentAdapter.Comment(user.getUid(), userName, text, System.currentTimeMillis(), isPrivate, replyToUserId, parentCommentId);
            commentsRef.push().setValue(comment).addOnSuccessListener(aVoid -> {
                etInput.setText("");
                etInput.setTag(null);
            });
        });

        bottomSheetDialog.show();
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
        LinearLayout layoutContact, layoutShare, layoutComment;
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
            layoutContact = itemView.findViewById(R.id.btn_item_contact_layout);
            layoutShare = itemView.findViewById(R.id.btn_item_share_layout);
            layoutComment = itemView.findViewById(R.id.btn_item_comment_layout);
            ivMoreOptions = itemView.findViewById(R.id.iv_more_options);
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
