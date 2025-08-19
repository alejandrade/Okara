package io.shrouded.okara.model;

import com.google.cloud.firestore.annotation.DocumentId;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.google.cloud.spring.data.firestore.Document;

import com.google.cloud.Timestamp;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Document(collectionName = "users")
public class User {
    
    @DocumentId
    private String id;
    
    private String username;
    
    private String email;
    
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private String location;
    private String website;
    
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
    private List<String> following = new ArrayList<>();
    private List<String> followers = new ArrayList<>();
    
    private Integer followingCount = 0;
    private Integer followersCount = 0;
    private Integer postsCount = 0;
    private Integer totalViewsCount = 0;
    
    private boolean verified = false;
    private boolean isPrivate = false;
    
    public User(String username, String email, String displayName) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.followingCount = 0;
        this.followersCount = 0;
        this.postsCount = 0;
        this.totalViewsCount = 0;
        this.verified = false;
        this.isPrivate = false;
    }
    
    // Ensure all fields are properly initialized in no-args constructor
    {
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.followingCount = 0;
        this.followersCount = 0;
        this.postsCount = 0;
        this.totalViewsCount = 0;
        this.verified = false;
        this.isPrivate = false;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
    }

    // Simple getters and setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    
    public String getBio() { return bio; }
    public void setBio(String bio) { this.bio = bio; }
    
    public String getProfileImageUrl() { return profileImageUrl; }
    public void setProfileImageUrl(String profileImageUrl) { this.profileImageUrl = profileImageUrl; }
    
    public String getBannerImageUrl() { return bannerImageUrl; }
    public void setBannerImageUrl(String bannerImageUrl) { this.bannerImageUrl = bannerImageUrl; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public List<String> getFollowing() { return following; }
    public void setFollowing(List<String> following) { this.following = following; }
    
    public List<String> getFollowers() { return followers; }
    public void setFollowers(List<String> followers) { this.followers = followers; }
    
    public Integer getFollowingCount() { return followingCount; }
    public void setFollowingCount(Integer followingCount) { this.followingCount = followingCount; }
    
    public Integer getFollowersCount() { return followersCount; }
    public void setFollowersCount(Integer followersCount) { this.followersCount = followersCount; }
    
    public Integer getPostsCount() { return postsCount; }
    public void setPostsCount(Integer postsCount) { this.postsCount = postsCount; }
    
    public Integer getTotalViewsCount() { return totalViewsCount; }
    public void setTotalViewsCount(Integer totalViewsCount) { this.totalViewsCount = totalViewsCount; }
    
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    
    public boolean isPrivate() { return isPrivate; }
    public void setPrivate(boolean isPrivate) { this.isPrivate = isPrivate; }
}