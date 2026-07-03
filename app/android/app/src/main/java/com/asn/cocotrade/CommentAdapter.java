package com.asn.cocotrade;

import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;
    private String postId;
    private OnCommentActionListener listener;

    public interface OnCommentActionListener {
        void onReplyPrivate(Comment comment);
    }

    public CommentAdapter(List<Comment> comments, String postId, OnCommentActionListener listener) {
        this.comments = comments;
        this.postId = postId;
        this.listener = listener;
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        Comment comment = comments.get(position);
        String currentUserId = FirebaseAuth.getInstance().getUid();

        RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) holder.itemView.getLayoutParams();
        if (params == null) {
            params = new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        }

        // Indentation for replies (thread style)
        if (comment.parentCommentId != null) {
            int margin = (int) (40 * holder.itemView.getContext().getResources().getDisplayMetrics().density);
            params.setMarginStart(margin);
        } else {
            params.setMarginStart(0);
        }
        holder.itemView.setLayoutParams(params);
        holder.itemView.setVisibility(View.VISIBLE);

        holder.tvUser.setText(comment.userName != null ? comment.userName : "User");
        holder.tvText.setText(comment.text != null ? comment.text : "");

        if (comment.isPrivate) {
            holder.tvText.setText("[Private] " + comment.text);
        }

        if (comment.userId != null) {
            FirebaseDatabase.getInstance().getReference("Users").child(comment.userId)
                    .addListenerForSingleValueEvent(new ValueEventListener() {
                        @Override
                        public void onDataChange(@NonNull DataSnapshot snapshot) {
                            String photoUrl = snapshot.child("profileImageUrl").getValue(String.class);
                            if (photoUrl != null && !photoUrl.isEmpty()) {
                                holder.tvInitial.setVisibility(View.GONE);
                                holder.ivProfilePhoto.setVisibility(View.VISIBLE);
                                Glide.with(holder.itemView.getContext())
                                        .load(photoUrl)
                                        .circleCrop()
                                        .into(holder.ivProfilePhoto);
                            } else {
                                holder.tvInitial.setVisibility(View.VISIBLE);
                                holder.ivProfilePhoto.setVisibility(View.GONE);
                                if (comment.userName != null && !comment.userName.isEmpty()) {
                                    holder.tvInitial.setText(String.valueOf(comment.userName.charAt(0)).toUpperCase());
                                } else {
                                    holder.tvInitial.setText("U");
                                }
                            }
                        }

                        @Override
                        public void onCancelled(@NonNull DatabaseError error) {
                            Log.e("CommentAdapter", "Profile image listener cancelled: " + error.getMessage());
                        }
                    });
        }

        // Adjust avatar size for replies
        ViewGroup.LayoutParams avatarParams = holder.layoutAvatar.getLayoutParams();
        float density = holder.itemView.getContext().getResources().getDisplayMetrics().density;
        if (comment.parentCommentId != null) {
            avatarParams.width = (int) (24 * density);
            avatarParams.height = (int) (24 * density);
            holder.tvInitial.setTextSize(10);
        } else {
            avatarParams.width = (int) (32 * density);
            avatarParams.height = (int) (32 * density);
            holder.tvInitial.setTextSize(12);
        }
        holder.layoutAvatar.setLayoutParams(avatarParams);

        long diff = System.currentTimeMillis() - comment.timestamp;
        String timeStr = "just now";
        if (diff > 60000) {
            long mins = diff / 60000;
            if (mins < 60) timeStr = mins + "m";
            else {
                long hours = mins / 60;
                if (hours < 24) timeStr = hours + "h";
                else timeStr = (hours / 24) + "d";
            }
        }
        holder.tvTime.setText(timeStr);

        if (currentUserId != null && currentUserId.equals(comment.userId)) {
            holder.ivDelete.setVisibility(View.VISIBLE);
            holder.ivDelete.setOnClickListener(v -> {
                FirebaseDatabase.getInstance().getReference("Comments")
                        .child(postId)
                        .child(comment.id)
                        .removeValue();
            });
        } else {
            holder.ivDelete.setVisibility(View.GONE);
        }

        holder.tvPrivate.setOnClickListener(v -> {
            if (listener != null) {
                listener.onReplyPrivate(comment);
            }
        });
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvUser, tvTime, tvText, tvPrivate;
        ImageView ivDelete, ivProfilePhoto;
        View layoutAvatar;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            layoutAvatar = itemView.findViewById(R.id.layout_comment_avatar);
            tvInitial = itemView.findViewById(R.id.tv_comment_initial);
            ivProfilePhoto = itemView.findViewById(R.id.iv_comment_profile_photo);
            tvUser = itemView.findViewById(R.id.tv_comment_user);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
            tvPrivate = itemView.findViewById(R.id.tv_comment_private);
            ivDelete = itemView.findViewById(R.id.iv_delete_comment);
        }
    }

    public static class Comment {
        public String id;
        public String userId;
        public String userName;
        public String text;
        public long timestamp;
        public boolean isPrivate;
        public String replyToUserId;
        public String parentCommentId;

        public Comment() {}

        public Comment(String userId, String userName, String text, long timestamp, boolean isPrivate, String replyToUserId, String parentCommentId) {
            this.userId = userId;
            this.userName = userName;
            this.text = text;
            this.timestamp = timestamp;
            this.isPrivate = isPrivate;
            this.replyToUserId = replyToUserId;
            this.parentCommentId = parentCommentId;
        }
    }
}
