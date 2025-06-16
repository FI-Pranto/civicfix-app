package com.fiprod.civicfix;

import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.HashMap;
import java.util.List;

public class IssueAdapter extends RecyclerView.Adapter<IssueAdapter.IssueViewHolder> {

    private List<Issue> issueList;
    private Context context;

    public IssueAdapter(Context context, List<Issue> issueList) {
        this.context = context;
        this.issueList = issueList;
    }

    @NonNull
    @Override
    public IssueViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.issue_card, parent, false);
        return new IssueViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull IssueViewHolder holder, int position) {
        Issue issue = issueList.get(position);
        holder.textTitle.setText(issue.title);

        // Truncate description
        if (issue.description != null && issue.description.length() > 18) {
            holder.textDescription.setText(issue.description.substring(0, 18) + "...");
        } else {
            holder.textDescription.setText(issue.description);
        }

        // Set status text
        holder.textStatus.setText(issue.status);

        // Set status background
        switch (issue.status.toUpperCase()) {
            case "PENDING":
                holder.textStatus.setBackgroundResource(R.drawable.status_pending);
                break;
            case "IN_PROGRESS":
                holder.textStatus.setBackgroundResource(R.drawable.status_in_progress);
                break;
            case "DONE":
                holder.textStatus.setBackgroundResource(R.drawable.status_resolved);
                break;
            default:
                holder.textStatus.setBackgroundResource(R.drawable.status_pending);
        }

        // Set timestamp and upvotes
        holder.textTimestamp.setText("⏱ " + TimeUtils.getRelativeTime(issue.timestamp));
        holder.textUpvoteCount.setText(String.valueOf(issue.upvotes));

        // Load issue image
        Glide.with(context).load(issue.imageUrl).into(holder.imageAvatar);

        // Handle upvote click
        holder.imageUpvote.setOnClickListener(v -> {
            String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();
            if (issue.upvotedBy == null) issue.upvotedBy = new HashMap<>();

            if (issue.upvotedBy.containsKey(userId)) {
                Toast.makeText(context, "You already upvoted", Toast.LENGTH_SHORT).show();
            } else {
                issue.upvotes++;
                issue.upvotedBy.put(userId, true);

                DatabaseReference issueRef = FirebaseDatabase.getInstance()
                        .getReference("issues")
                        .child(issue.id);

                issueRef.child("upvotes").setValue(issue.upvotes);
                issueRef.child("upvotedBy").setValue(issue.upvotedBy);

                holder.textUpvoteCount.setText(String.valueOf(issue.upvotes));
            }
        });

        // Handle card click
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, IssueDetailsActivity.class);
            intent.putExtra("id", issue.id);
            intent.putExtra("title", issue.title);
            intent.putExtra("description", issue.description);
            intent.putExtra("status", issue.status);
            intent.putExtra("category", issue.category);
            intent.putExtra("area", issue.area);
            intent.putExtra("imageUrl", issue.imageUrl);
            intent.putExtra("upvotes", issue.upvotes);
            intent.putExtra("submittedBy", issue.submittedBy != null ? issue.submittedBy.get("name") : "Unknown");
            intent.putExtra("timestamp", issue.timestamp);
            intent.putExtra("handledBy", issue.handledBy != null ? issue.handledBy : "Not assigned");

            context.startActivity(intent);
        });
    }

    @Override
    public int getItemCount() {
        return issueList.size();
    }

    static class IssueViewHolder extends RecyclerView.ViewHolder {
        TextView textTitle, textDescription, textStatus, textTimestamp, textUpvoteCount;
        ImageView imageAvatar, imageUpvote;

        public IssueViewHolder(@NonNull View itemView) {
            super(itemView);
            textTitle = itemView.findViewById(R.id.text_title);
            textDescription = itemView.findViewById(R.id.text_description);
            textStatus = itemView.findViewById(R.id.text_status);
            textTimestamp = itemView.findViewById(R.id.text_timestamp);
            textUpvoteCount = itemView.findViewById(R.id.text_upvote_count);
            imageAvatar = itemView.findViewById(R.id.image_avatar);
            imageUpvote = itemView.findViewById(R.id.image_upvote);
        }
    }
}
