package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.ArrayList;

@Data
@NoArgsConstructor
public class User {

    private String id;
    private String email;
    private String displayName = "Anon";
    private String bio;
    private String profileImageUrl;
    private String bannerImageUrl;
    private String location;
    private String website;
    private String fcmToken;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private List<String> following = new ArrayList<>();
    private List<String> followers = new ArrayList<>();

    private Integer followingCount = 0;
    private Integer followersCount = 0;
    private Integer postsCount = 0;
    private Integer totalViewsCount = 0;

    private boolean verified;
    private boolean isPrivate;

    private List<UserChatroom> chatrooms = new ArrayList<>();

}