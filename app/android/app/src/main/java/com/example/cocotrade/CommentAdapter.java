package com.example.cocotrade;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.CommentViewHolder> {

    private List<Comment> comments;

    public CommentAdapter(List<Comment> comments) {
        this.comments = comments;
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
        holder.tvUser.setText(comment.userName != null ? comment.userName : "User");
        holder.tvText.setText(comment.text != null ? comment.text : "");

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
    }

    @Override
    public int getItemCount() {
        return comments.size();
    }

    public static class CommentViewHolder extends RecyclerView.ViewHolder {
        TextView tvInitial, tvUser, tvTime, tvText;

        public CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            tvInitial = itemView.findViewById(R.id.tv_comment_initial);
            tvUser = itemView.findViewById(R.id.tv_comment_user);
            tvTime = itemView.findViewById(R.id.tv_comment_time);
            tvText = itemView.findViewById(R.id.tv_comment_text);
        }
    }

    public static class Comment {
        public String id;
        public String userId;
        public String userName;
        public String text;
        public long timestamp;

        public Comment() {}

        public Comment(String userId, String userName, String text, long timestamp) {
            this.userId = userId;
            this.userName = userName;
            this.text = text;
            this.timestamp = timestamp;
        }
    }
}
