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

    private List<String> following;
    private List<String> followers;

    private Integer followingCount;
    private Integer followersCount;
    private Integer postsCount;
    private Integer totalViewsCount;

    private boolean verified;
    private boolean isPrivate;

    private List<UserChatroom> chatrooms = new ArrayList<>();

}