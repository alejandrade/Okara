package io.shrouded.okara.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.cloud.Timestamp;
import java.util.ArrayList;
import java.util.List;

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
    private List<String> imageUrls = new ArrayList<>();
    private String videoUrl;
    
    private FeedType type = FeedType.POST;
    
    // Hierarchical structure for comments
    private String parentId; // null for original posts, feedId for comments/replies
    private String rootId; // always points to the original post
    
    private Timestamp createdAt;
    private Timestamp updatedAt;
    
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
    private Double baseEngagementScore = 0.0; // Global engagement score
    private Integer distinctCommentersCount = 0; // Count of unique users who commented
    private List<String> niches = new ArrayList<>(); // For future: ["tech", "startup", "ai"]
    
    // For retweets and quote tweets
    private String originalPostId;
    private String quoteTweetComment;
    
    // Content moderation
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

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }
    
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    
    public String getAuthorDisplayName() { return authorDisplayName; }
    public void setAuthorDisplayName(String authorDisplayName) { this.authorDisplayName = authorDisplayName; }
    
    public String getAuthorProfileImageUrl() { return authorProfileImageUrl; }
    public void setAuthorProfileImageUrl(String authorProfileImageUrl) { this.authorProfileImageUrl = authorProfileImageUrl; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public List<String> getImageUrls() { return imageUrls; }
    public void setImageUrls(List<String> imageUrls) { this.imageUrls = imageUrls; }
    
    public String getVideoUrl() { return videoUrl; }
    public void setVideoUrl(String videoUrl) { this.videoUrl = videoUrl; }
    
    public FeedType getType() { return type; }
    public void setType(FeedType type) { this.type = type; }
    
    public String getParentId() { return parentId; }
    public void setParentId(String parentId) { this.parentId = parentId; }
    
    public String getRootId() { return rootId; }
    public void setRootId(String rootId) { this.rootId = rootId; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
    
    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
    
    public List<String> getLikedBy() { return likedBy; }
    public void setLikedBy(List<String> likedBy) { this.likedBy = likedBy; }
    
    public List<String> getDislikedBy() { return dislikedBy; }
    public void setDislikedBy(List<String> dislikedBy) { this.dislikedBy = dislikedBy; }
    
    public List<String> getRetweetedBy() { return retweetedBy; }
    public void setRetweetedBy(List<String> retweetedBy) { this.retweetedBy = retweetedBy; }
    
    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
    
    public Integer getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(Integer dislikesCount) { this.dislikesCount = dislikesCount; }
    
    public Integer getRetweetsCount() { return retweetsCount; }
    public void setRetweetsCount(Integer retweetsCount) { this.retweetsCount = retweetsCount; }
    
    public Integer getCommentsCount() { return commentsCount; }
    public void setCommentsCount(Integer commentsCount) { this.commentsCount = commentsCount; }
    
    public Integer getViewsCount() { return viewsCount; }
    public void setViewsCount(Integer viewsCount) { this.viewsCount = viewsCount; }
    
    public Double getBaseEngagementScore() { return baseEngagementScore; }
    public void setBaseEngagementScore(Double baseEngagementScore) { this.baseEngagementScore = baseEngagementScore; }
    
    public Integer getDistinctCommentersCount() { return distinctCommentersCount; }
    public void setDistinctCommentersCount(Integer distinctCommentersCount) { this.distinctCommentersCount = distinctCommentersCount; }
    
    public List<String> getNiches() { return niches; }
    public void setNiches(List<String> niches) { this.niches = niches; }
    
    public String getOriginalPostId() { return originalPostId; }
    public void setOriginalPostId(String originalPostId) { this.originalPostId = originalPostId; }
    
    public String getQuoteTweetComment() { return quoteTweetComment; }
    public void setQuoteTweetComment(String quoteTweetComment) { this.quoteTweetComment = quoteTweetComment; }
    
    public boolean isPinned() { return isPinned; }
    public void setPinned(boolean isPinned) { this.isPinned = isPinned; }
    
    public List<String> getHashtags() { return hashtags; }
    public void setHashtags(List<String> hashtags) { this.hashtags = hashtags; }
    
    public List<String> getMentions() { return mentions; }
    public void setMentions(List<String> mentions) { this.mentions = mentions; }
}