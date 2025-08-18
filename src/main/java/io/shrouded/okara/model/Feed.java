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
@Document(collection = "feed")
public class Feed {
    
    @Id
    private String id;
    
    @Indexed
    private String authorId;
    private String authorUsername;
    private String authorDisplayName;
    private String authorProfileImageUrl;
    
    private String content;
    private List<String> imageUrls = new ArrayList<>();
    private String videoUrl;
    
    @Indexed
    private FeedType type = FeedType.POST;
    
    // Hierarchical structure for comments
    @Indexed
    private String parentId; // null for original posts, feedId for comments/replies
    private String rootId; // always points to the original post
    
    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Engagement metrics
    private List<String> likedBy = new ArrayList<>();
    private List<String> dislikedBy = new ArrayList<>();
    private List<String> retweetedBy = new ArrayList<>();
    
    private Integer likesCount = 0;
    private Integer dislikesCount = 0;
    private Integer retweetsCount = 0;
    private Integer commentsCount = 0;
    private Integer viewsCount = 0;
    
    // Algorithm scoring (for future personalization)
    @Indexed
    private Double baseEngagementScore = 0.0; // Global engagement score
    private Integer distinctCommentersCount = 0; // Count of unique users who commented
    private List<String> niches = new ArrayList<>(); // For future: ["tech", "startup", "ai"]
    
    // For retweets and quote tweets
    private String originalPostId;
    private String quoteTweetComment;
    
    // Content moderation
    private boolean isDeleted = false;
    private boolean isPinned = false;
    private List<String> hashtags = new ArrayList<>();
    private List<String> mentions = new ArrayList<>();
    
    public enum FeedType {
        POST, COMMENT, RETWEET, QUOTE_TWEET
    }
    
    public Feed(String authorId, String authorUsername, String content, FeedType type) {
        this.authorId = authorId;
        this.authorUsername = authorUsername;
        this.content = content;
        this.type = type;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        this.likesCount = 0;
        this.dislikesCount = 0;
        this.retweetsCount = 0;
        this.commentsCount = 0;
        this.viewsCount = 0;
        this.isDeleted = false;
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