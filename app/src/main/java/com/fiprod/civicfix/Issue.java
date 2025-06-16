package com.fiprod.civicfix;

import java.util.Map;

public class Issue {
    public String id;
    public String title;
    public String description;
    public String status;
    public String imageUrl;
    public String timestamp;
    public int upvotes;

    public String area;
    public String category;
    public String handledBy;

    public Map<String, Boolean> upvotedBy; // userId -> true
    public Map<String, String> submittedBy; // e.g., { userId: "...", name: "..." }

    public Issue() {
        // Required by Firebase
    }

    public Issue(String id, String title, String description, String status, String imageUrl,
                 String timestamp, int upvotes, Map<String, Boolean> upvotedBy,
                 String area, String category, Map<String, String> submittedBy, String handledBy) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.imageUrl = imageUrl;
        this.timestamp = timestamp;
        this.upvotes = upvotes;
        this.upvotedBy = upvotedBy;
        this.area = area;
        this.category = category;
        this.submittedBy = submittedBy;
        this.handledBy = handledBy;
    }
}
