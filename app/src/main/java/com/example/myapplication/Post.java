package com.example.myapplication;

public class Post {
    private String mushroomType;
    private String description;
    private String imageUrl;
    private String userId;
    private String username;
    private double latitude;
    private double longitude;
    private long timestamp;
    private String verified; // default: "Not Verified"
    private String location; // optional

    public Post() {} // Firestore requires empty constructor

    public Post(String mushroomType, String description, String imageUrl,
                String userId, String username, double latitude, double longitude,
                long timestamp, String verified, String location) {
        this.mushroomType = mushroomType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.userId = userId;
        this.username = username;
        this.latitude = latitude;
        this.longitude = longitude;
        this.timestamp = timestamp;
        this.verified = verified;
        this.location = location;
    }

    // Getters
    public String getMushroomType() { return mushroomType; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
    public String getUsername() { return username; }
    public double getLatitude() { return latitude; }
    public double getLongitude() { return longitude; }
    public long getTimestamp() { return timestamp; }
    public String getVerified() { return verified; }
    public String getLocation() { return location; }
}
