package com.example.myapplication;

public class Post {
    private String mushroomType;
    private String description;
    private String imageUrl;
    private String userId;

    public Post() {}

    public Post(String mushroomType, String description, String imageUrl, String userId) {
        this.mushroomType = mushroomType;
        this.description = description;
        this.imageUrl = imageUrl;
        this.userId = userId;
    }

    public String getMushroomType() { return mushroomType; }
    public String getDescription() { return description; }
    public String getImageUrl() { return imageUrl; }
    public String getUserId() { return userId; }
}
