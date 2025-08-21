package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@Document(collectionName = "users")
public class User {

    @DocumentId
    private String id;
    private String firebaseUid;

    private String username;
    private String email;
    private String displayName;
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private String location;
    private String website;
    private String fcmToken;

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
}