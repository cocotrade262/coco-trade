package com.example.cocotrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;
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

        // Private visibility logic: visible to sender or the intended recipient
        boolean isVisible = !comment.isPrivate ||
                           (currentUserId != null && (currentUserId.equals(comment.userId) || currentUserId.equals(comment.replyToUserId)));

        if (!isVisible) {
            holder.itemView.setVisibility(View.GONE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(0, 1)); // Minimal height
            return;
        } else {
            holder.itemView.setVisibility(View.VISIBLE);
            holder.itemView.setLayoutParams(new RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        }

        holder.tvUser.setText(comment.userName != null ? comment.userName : "User");
        holder.tvText.setText(comment.text != null ? comment.text : "");

        if (comment.isPrivate) {
            holder.tvText.setText("[Private] " + comment.text);
        }

        if (comment.userName != null && !comment.userName.isEmpty()) {
            holder.tvInitial.setText(String.valueOf(comment.userName.charAt(0)).toUpperCase());
        } else {
            holder.tvInitial.setText("U");
        }

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
        ImageView ivDelete;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tv_comment_initial);
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

        public Comment() {}

        public Comment(String userId, String userName, String text, long timestamp, boolean isPrivate, String replyToUserId) {
            this.userId = userId;
            this.userName = userName;
            this.text = text;
            this.timestamp = timestamp;
            this.isPrivate = isPrivate;
            this.replyToUserId = replyToUserId;
        }
    }
}
