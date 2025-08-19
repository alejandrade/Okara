package io.shrouded.okara.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.cloud.Timestamp;
import java.util.ArrayList;
import java.util.List;

@NoArgsConstructor
@Document(collectionName = "user_feeds")
public class UserFeed {
    
    @DocumentId
    private String id; // Format: {userId}_{feedType}
    
    private String userId;
    private FeedType feedType;
    
    private List<FeedItem> items = new ArrayList<>();
    private Timestamp lastUpdated;
    
    // Pagination cursors
    private String nextCursor;
    private String prevCursor;
    
    // Feed metadata
    private Integer totalItems = 0;
    private Timestamp createdAt;
    
    public enum FeedType {
        PERSONAL  // Simple personal feed - chronological for now
    }
    
    @NoArgsConstructor
    public static class FeedItem {
        private String postId;
        private String authorId;
        private String authorUsername;
        private String authorDisplayName;
        private String authorProfileImageUrl;
        
        private String content;
        private List<String> imageUrls = new ArrayList<>();
        private String videoUrl;
        
        private Feed.FeedType postType; // POST, RETWEET, etc.
        private Timestamp createdAt;
        private Timestamp addedToFeedAt;
        
        // Engagement data (cached for performance)
        private Integer likesCount = 0;
        private Integer retweetsCount = 0;
        private Integer commentsCount = 0;
        
        // Algorithm scoring
        private Double algorithmScore = 0.0;
        private List<String> relevanceTags = new ArrayList<>(); // Why this was shown to user
        private String reasonShown; // "Liked by people you follow", "Trending in your area", etc.
        
        // For retweets/quotes
        private String originalPostId;
        private String retweetComment;
        
        public FeedItem(Feed post) {
            this.postId = post.getId();
            this.authorId = post.getAuthorId();
            this.authorUsername = post.getAuthorUsername();
            this.authorDisplayName = post.getAuthorDisplayName();
            this.authorProfileImageUrl = post.getAuthorProfileImageUrl();
            
            this.content = post.getContent();
            this.imageUrls = post.getImageUrls() != null ? post.getImageUrls() : new ArrayList<>();
            this.videoUrl = post.getVideoUrl();
            
            this.postType = post.getType();
            this.createdAt = post.getCreatedAt();
            this.addedToFeedAt = Timestamp.now();
            
            this.likesCount = post.getLikesCount();
            this.retweetsCount = post.getRetweetsCount();
            this.commentsCount = post.getCommentsCount();
            
            this.algorithmScore = post.getBaseEngagementScore();
            
            this.originalPostId = post.getOriginalPostId();
            this.retweetComment = post.getQuoteTweetComment();
        }

        // Getters and Setters for FeedItem
        public String getPostId() { return postId; }
        public void setPostId(String postId) { this.postId = postId; }
        
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
        
        public Feed.FeedType getPostType() { return postType; }
        public void setPostType(Feed.FeedType postType) { this.postType = postType; }
        
        public Timestamp getCreatedAt() { return createdAt; }
        public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
        
        public Timestamp getAddedToFeedAt() { return addedToFeedAt; }
        public void setAddedToFeedAt(Timestamp addedToFeedAt) { this.addedToFeedAt = addedToFeedAt; }
        
        public Integer getLikesCount() { return likesCount; }
        public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }
        
        public Integer getRetweetsCount() { return retweetsCount; }
        public void setRetweetsCount(Integer retweetsCount) { this.retweetsCount = retweetsCount; }
        
        public Integer getCommentsCount() { return commentsCount; }
        public void setCommentsCount(Integer commentsCount) { this.commentsCount = commentsCount; }
        
        public Double getAlgorithmScore() { return algorithmScore; }
        public void setAlgorithmScore(Double algorithmScore) { this.algorithmScore = algorithmScore; }
        
        public List<String> getRelevanceTags() { return relevanceTags; }
        public void setRelevanceTags(List<String> relevanceTags) { this.relevanceTags = relevanceTags; }
        
        public String getReasonShown() { return reasonShown; }
        public void setReasonShown(String reasonShown) { this.reasonShown = reasonShown; }
        
        public String getOriginalPostId() { return originalPostId; }
        public void setOriginalPostId(String originalPostId) { this.originalPostId = originalPostId; }
        
        public String getRetweetComment() { return retweetComment; }
        public void setRetweetComment(String retweetComment) { this.retweetComment = retweetComment; }
    }
    
    public UserFeed(String userId, FeedType feedType) {
        this.id = generateId(userId, feedType);
        this.userId = userId;
        this.feedType = feedType;
        this.items = new ArrayList<>();
        this.createdAt = Timestamp.now();
        this.lastUpdated = Timestamp.now();
        this.totalItems = 0;
    }
    
    public static String generateId(String userId, FeedType feedType) {
        return String.format("%s_%s", userId, feedType.name().toLowerCase());
    }
    
    private static final int MAX_FEED_SIZE = 500;
    
    public void addItem(FeedItem item) {
        this.items.add(0, item); // Add to beginning (most recent first)
        this.lastUpdated = Timestamp.now();
        
        // FIFO: Remove oldest items if we exceed max size
        if (this.items.size() > MAX_FEED_SIZE) {
            this.items = this.items.subList(0, MAX_FEED_SIZE);
        }
        
        this.totalItems = this.items.size();
    }
    
    public void addItems(List<FeedItem> newItems) {
        // Add all items to beginning and sort by addedToFeedAt or algorithm score
        this.items.addAll(0, newItems);
        
        // Sort chronologically (most recent first)
        this.items.sort((a, b) -> b.getCreatedAt().toDate().compareTo(a.getCreatedAt().toDate()));
        
        this.lastUpdated = Timestamp.now();
        
        // FIFO: Remove oldest items if we exceed max size
        if (this.items.size() > MAX_FEED_SIZE) {
            this.items = this.items.subList(0, MAX_FEED_SIZE);
        }
        
        this.totalItems = this.items.size();
    }
    
    public List<FeedItem> getItemsPage(int offset, int limit) {
        int fromIndex = Math.min(offset, this.items.size());
        int toIndex = Math.min(offset + limit, this.items.size());
        
        if (fromIndex >= toIndex) {
            return new ArrayList<>();
        }
        
        return new ArrayList<>(this.items.subList(fromIndex, toIndex));
    }
    
    public List<FeedItem> getItemsSince(String sinceId, int limit) {
        if (sinceId == null || sinceId.isEmpty()) {
            // No cursor, return from the beginning
            return getItemsPage(0, limit);
        }
        
        // Find the position of the sinceId item
        int startIndex = 0;
        for (int i = 0; i < this.items.size(); i++) {
            if (this.items.get(i).getPostId().equals(sinceId)) {
                startIndex = i + 1; // Start after the sinceId item
                break;
            }
        }
        
        return getItemsPage(startIndex, limit);
    }

    // Getters and Setters for UserFeed
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    
    public FeedType getFeedType() { return feedType; }
    public void setFeedType(FeedType feedType) { this.feedType = feedType; }
    
    
    public List<FeedItem> getItems() { return items; }
    public void setItems(List<FeedItem> items) { this.items = items; }
    
    public Timestamp getLastUpdated() { return lastUpdated; }
    public void setLastUpdated(Timestamp lastUpdated) { this.lastUpdated = lastUpdated; }
    
    public String getNextCursor() { return nextCursor; }
    public void setNextCursor(String nextCursor) { this.nextCursor = nextCursor; }
    
    public String getPrevCursor() { return prevCursor; }
    public void setPrevCursor(String prevCursor) { this.prevCursor = prevCursor; }
    
    public Integer getTotalItems() { return totalItems; }
    public void setTotalItems(Integer totalItems) { this.totalItems = totalItems; }
    
    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }
}