package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import io.shrouded.okara.enums.FeedType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Feed {

    private String id;

    private String authorId;
    private String authorDisplayName;
    private String authorProfileImageUrl;

    private String content;
    private List<String> imageUrls;
    private String videoUrl;

    private FeedType type;

    private String parentId;
    private String rootId;

    private Timestamp createdAt;
    private Timestamp updatedAt;

    private List<String> likedBy;
    private List<String> dislikedBy;

    private Integer likesCount;
    private Integer dislikesCount;
    private Integer commentsCount;
    private Integer viewsCount;

    private Integer distinctCommentersCount;
    private List<String> niches;

    private String originalPostId;

    private List<String> hashtags;
    private List<String> mentions;
    private List<String> chatroomIds;

    public Feed(String authorId, String content, FeedType type) {
        this.authorId = authorId;
        this.content = content;
        this.type = type;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
        this.likesCount = 0;
        this.dislikesCount = 0;
        this.commentsCount = 0;
        this.viewsCount = 0;
        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.hashtags = new ArrayList<>();
        this.mentions = new ArrayList<>();
        this.distinctCommentersCount = 0;
        this.niches = new ArrayList<>();
        chatroomIds = new ArrayList<>();
    }
}