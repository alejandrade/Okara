package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
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

    private List<String> following;
    private List<String> followers;

    private Integer followingCount;
    private Integer followersCount;
    private Integer postsCount;
    private Integer totalViewsCount;

    private boolean verified;
    private boolean isPrivate;

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
}