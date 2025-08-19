package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import io.shrouded.okara.enums.FeedType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@Document(collectionName = "feeds")
public class Feed {

    @DocumentId
    private String id;

    private String authorId;
    private String authorUsername;
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
    private List<String> retweetedBy;

    private Integer likesCount;
    private Integer dislikesCount;
    private Integer retweetsCount;
    private Integer commentsCount;
    private Integer viewsCount;

    private Double baseEngagementScore;
    private Integer distinctCommentersCount;
    private List<String> niches;

    private String originalPostId;
    private String quoteTweetComment;

    private boolean isPinned;
    private List<String> hashtags;
    private List<String> mentions;


    public Feed(String authorId, String authorUsername, String content, FeedType type) {
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.content = content;
        this.type = type;
        this.createdAt = Timestamp.now();
        this.updatedAt = Timestamp.now();
        this.likesCount = 0;
        this.dislikesCount = 0;
        this.retweetsCount = 0;
        this.commentsCount = 0;
        this.viewsCount = 0;
        this.isPinned = false;
        this.likedBy = new ArrayList<>();
        this.dislikedBy = new ArrayList<>();
        this.retweetedBy = new ArrayList<>();
        this.imageUrls = new ArrayList<>();
        this.hashtags = new ArrayList<>();
        this.mentions = new ArrayList<>();
        this.baseEngagementScore = 0.0;
        this.distinctCommentersCount = 0;
        this.niches = new ArrayList<>();
    }
}