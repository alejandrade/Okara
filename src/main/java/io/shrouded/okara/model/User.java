package io.shrouded.okara.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collection = "users")
public class User {
    
    @Id
    private String id;
    
    @Indexed(unique = true)
    private String username;
    
    @Indexed(unique = true)
    private String email;
    
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private String location;
    private String website;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    private List<String> following = new ArrayList<>();
    private List<String> followers = new ArrayList<>();
    
    private Integer followingCount = 0;
    private Integer followersCount = 0;
    private Integer postsCount = 0;
    
    private boolean verified = false;
    private boolean isPrivate = false;
    
    public User(String username, String email, String displayName) {
        this.username = username;
        this.email = email;
        this.displayName = displayName;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.following = new ArrayList<>();
        this.followers = new ArrayList<>();
        this.followingCount = 0;
        this.followersCount = 0;
        this.postsCount = 0;
        this.verified = false;
        this.isPrivate = false;
    }
}