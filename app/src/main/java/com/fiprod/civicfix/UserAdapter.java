package com.fiprod.civicfix;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;

import java.util.List;

public class UserAdapter extends RecyclerView.Adapter<UserAdapter.UserViewHolder> {

    private List<User> userList;

    public UserAdapter(List<User> userList) {
        this.userList = userList;
    }

    @NonNull
    @Override
    public UserViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the layout for each item in the list
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.user_card, parent, false);
        return new UserViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull UserViewHolder holder, int position) {
        // Get the user data at the current position
        User user = userList.get(position);

        // Set name with null check
        String fullName = user.getFullName();
        if (fullName != null && !fullName.trim().isEmpty()) {
            holder.nameTextView.setText(fullName.trim());
        } else {
            holder.nameTextView.setText("Unknown User");
        }

        // Set role with null check
        String role = user.getRole();
        if (role != null && !role.trim().isEmpty()) {
            holder.roleTextView.setText(role);
        } else {
            holder.roleTextView.setText("No Role Assigned");
        }

        // Load profile image with Glide, with improved error handling
        String profileImageUrl = user.getProfileImageUrl();
        if (profileImageUrl != null && !profileImageUrl.trim().isEmpty()) {
            Glide.with(holder.profileImageView.getContext())
                    .load(profileImageUrl)
                    .placeholder(R.drawable.circle_bg) // Show placeholder while loading
                    .error(R.drawable.circle_bg) // Show default image if loading fails
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(holder.profileImageView);
        } else {
            // Set a default profile image if URL is null or empty
            Glide.with(holder.profileImageView.getContext())
                    .load(R.drawable.circle_bg)
                    .into(holder.profileImageView);
        }

        // On card click, open the Profile activity
        holder.userCard.setOnClickListener(v -> {
            try {
                Intent intent = new Intent(v.getContext(), UserProfileActivity.class);
                // Ensure that the user ID is passed correctly to the next activity
                String userId = user.getUserId();
                if (userId != null && !userId.trim().isEmpty()) {
                    intent.putExtra("USER_ID", userId);
                    v.getContext().startActivity(intent);
                } else {
                    Toast.makeText(v.getContext(), "User ID is missing!", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                Toast.makeText(v.getContext(), "Error opening user profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public int getItemCount() {
        // Return the size of the user list, with null check
        return userList != null ? userList.size() : 0;
    }

    public void updateUsers(List<User> newUserList) {
        // Update the user list and notify the adapter that the data has changed
        if (newUserList != null) {
            this.userList = newUserList;
            notifyDataSetChanged();
        }
    }

    // ViewHolder class that holds the views for each user item
    public static class UserViewHolder extends RecyclerView.ViewHolder {
        TextView nameTextView, roleTextView;
        ImageView profileImageView;
        CardView userCard;

        public UserViewHolder(@NonNull View itemView) {
            super(itemView);
            // Find the views by their IDs
            nameTextView = itemView.findViewById(R.id.text_name);
            roleTextView = itemView.findViewById(R.id.text_role);
            profileImageView = itemView.findViewById(R.id.image_avatar);
            userCard = itemView.findViewById(R.id.user_card);
        }
    }
}