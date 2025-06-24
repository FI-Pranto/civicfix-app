package com.fiprod.civicfix;

public class User {
    private String firstName;
    private String lastName;
    private String role;
    private String profileImageUrl;
    private String userId;

    // Default constructor (required for Firestore)
    public User() {
    }

    // Constructor with parameters
    public User(String firstName, String lastName, String role, String profileImageUrl, String userId) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.role = role;
        this.profileImageUrl = profileImageUrl;
        this.userId = userId;
    }

    // Getters
    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getRole() {
        return role;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public String getUserId() {
        return userId;
    }

    // Setters (helpful for Firestore)
    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    // Get full name
    public String getFullName() {
        return firstName + " " + lastName;
    }
}