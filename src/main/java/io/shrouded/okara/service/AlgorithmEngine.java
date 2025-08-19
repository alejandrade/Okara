package io.shrouded.okara.service;

import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import com.google.cloud.Timestamp;
import io.shrouded.okara.util.TimestampUtils;
import java.util.*;

@Service
@Slf4j
public class AlgorithmEngine {
    
    // Base scoring weights
    private static final double ENGAGEMENT_WEIGHT = 0.3;
    private static final double RECENCY_WEIGHT = 0.2;
    private static final double AUTHOR_RELATIONSHIP_WEIGHT = 0.25;
    private static final double CONTENT_RELEVANCE_WEIGHT = 0.15;
    private static final double DIVERSITY_WEIGHT = 0.1;
    
    // Engagement thresholds for discovery
    private static final int MIN_LIKES_FOR_DISCOVERY = 5;
    private static final int MIN_RETWEETS_FOR_DISCOVERY = 2;
    private static final double MIN_ENGAGEMENT_SCORE_FOR_DISCOVERY = 10.0;
    
    /**
     * Calculate personalized score for a post based on user preferences and behavior
     */
    public double calculatePersonalizedScore(Feed post, User user) {
        try {
            double score = 0.0;
            
            // 1. Base engagement score (30%)
            score += calculateEngagementScore(post) * ENGAGEMENT_WEIGHT;
            
            // 2. Recency score (20%)
            score += calculateRecencyScore(post) * RECENCY_WEIGHT;
            
            // 3. Author relationship score (25%)
            score += calculateAuthorRelationshipScore(post, user) * AUTHOR_RELATIONSHIP_WEIGHT;
            
            // 4. Content relevance score (15%)
            score += calculateContentRelevanceScore(post, user) * CONTENT_RELEVANCE_WEIGHT;
            
            // 5. Diversity bonus (10%)
            score += calculateDiversityScore(post, user) * DIVERSITY_WEIGHT;
            
            // Apply penalties/bonuses
            score *= applyPenaltiesAndBonuses(post, user);
            
            // Ensure score is non-negative
            score = Math.max(0.0, score);
            
            log.debug("Calculated personalized score {} for post {} and user {}", score, post.getId(), user.getId());
            return score;
            
        } catch (Exception e) {
            log.error("Error calculating personalized score for post {} and user {}: {}", 
                post.getId(), user.getId(), e.getMessage());
            return 0.0;
        }
    }
    
    /**
     * Determine if a post should be added to discovery feeds
     */
    public boolean shouldAddToDiscovery(Feed post) {
        // High engagement posts
        boolean highEngagement = post.getLikesCount() >= MIN_LIKES_FOR_DISCOVERY 
            || post.getRetweetsCount() >= MIN_RETWEETS_FOR_DISCOVERY
            || post.getBaseEngagementScore() >= MIN_ENGAGEMENT_SCORE_FOR_DISCOVERY;
            
        // Trending hashtags (simplified)
        boolean hasTrendingHashtags = post.getHashtags().stream()
            .anyMatch(tag -> isTrendingHashtag(tag));
            
        // Recent and engaging
        boolean recentAndEngaging = TimestampUtils.isRecent(post.getCreatedAt(), 2) && post.getBaseEngagementScore() > 5.0;
        
        return highEngagement || hasTrendingHashtags || recentAndEngaging;
    }
    
    /**
     * Get explanation for why this post was shown to the user
     */
    public String getReasonShown(Feed post, User user) {
        // Check various reasons in priority order
        
        if (user.getFollowing().contains(post.getAuthorId())) {
            return "From " + post.getAuthorUsername() + " (following)";
        }
        
        if (hasCommonInterests(post, user)) {
            return "Based on your interests in " + String.join(", ", getCommonInterests(post, user));
        }
        
        if (post.getBaseEngagementScore() > 20.0) {
            return "Trending post with high engagement";
        }
        
        if (post.getHashtags().stream().anyMatch(this::isTrendingHashtag)) {
            String trendingTag = post.getHashtags().stream()
                .filter(this::isTrendingHashtag)
                .findFirst()
                .orElse("");
            return "Trending topic: #" + trendingTag;
        }
        
        if (post.getMentions().contains(user.getUsername())) {
            return "You were mentioned";
        }
        
        return "Recommended for you";
    }
    
    /**
     * Get relevance tags explaining why this post is relevant
     */
    public List<String> getRelevanceTags(Feed post, User user) {
        List<String> tags = new ArrayList<>();
        
        if (user.getFollowing().contains(post.getAuthorId())) {
            tags.add("following");
        }
        
        if (post.getBaseEngagementScore() > 15.0) {
            tags.add("high_engagement");
        }
        
        if (TimestampUtils.isRecent(post.getCreatedAt(), 1)) {
            tags.add("recent");
        }
        
        if (hasCommonInterests(post, user)) {
            tags.add("interests_match");
        }
        
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            tags.add("has_media");
        }
        
        if (post.getHashtags().stream().anyMatch(this::isTrendingHashtag)) {
            tags.add("trending_hashtag");
        }
        
        return tags;
    }
    
    private double calculateEngagementScore(Feed post) {
        // Normalize engagement metrics (0-100 scale)
        double likes = Math.min(post.getLikesCount() * 2.0, 100.0); // Cap at 50 likes = 100 points
        double retweets = Math.min(post.getRetweetsCount() * 5.0, 100.0); // Cap at 20 retweets = 100 points
        double comments = Math.min(post.getCommentsCount() * 3.0, 100.0); // Cap at ~33 comments = 100 points
        
        // Weight comments higher as they show more engagement
        return (likes + retweets * 1.5 + comments * 2.0) / 4.5;
    }
    
    private double calculateRecencyScore(Feed post) {
        long hoursOld = TimestampUtils.hoursUntilNow(post.getCreatedAt());
        
        // Fresh posts (0-6h): 100 points
        if (hoursOld <= 6) return 100.0;
        
        // Recent posts (6-24h): 80-100 points
        if (hoursOld <= 24) return 100.0 - ((hoursOld - 6) / 18.0) * 20.0;
        
        // Day old posts (1-3 days): 40-80 points
        if (hoursOld <= 72) return 80.0 - ((hoursOld - 24) / 48.0) * 40.0;
        
        // Older posts decay rapidly
        return Math.max(0, 40.0 - ((hoursOld - 72) / 168.0) * 40.0);
    }
    
    private double calculateAuthorRelationshipScore(Feed post, User user) {
        String authorId = post.getAuthorId();
        
        // User's own posts get highest score
        if (authorId.equals(user.getId())) {
            return 100.0;
        }
        
        // Following the author gets high score
        if (user.getFollowing().contains(authorId)) {
            return 90.0;
        }
        
        // Mutual connections (simplified - would need more data)
        // For now, use a simple heuristic based on author's follower count
        // Popular authors (but not too popular) get medium score
        return 30.0; // Default score for non-followed authors
    }
    
    private double calculateContentRelevanceScore(Feed post, User user) {
        double score = 0.0;
        
        // Check for common interests (hashtags, mentions)
        if (hasCommonInterests(post, user)) {
            score += 50.0;
        }
        
        // Check if user was mentioned
        if (post.getMentions().contains(user.getUsername())) {
            score += 80.0;
        }
        
        // Check for trending topics
        if (post.getHashtags().stream().anyMatch(this::isTrendingHashtag)) {
            score += 30.0;
        }
        
        // Media content gets slight boost
        if ((post.getImageUrls() != null && !post.getImageUrls().isEmpty()) || post.getVideoUrl() != null) {
            score += 10.0;
        }
        
        return Math.min(score, 100.0);
    }
    
    private double calculateDiversityScore(Feed post, User user) {
        // Encourage diversity in content types and authors
        double score = 50.0; // Base diversity score
        
        // Different post types get different scores
        switch (post.getType()) {
            case POST -> score += 0.0; // Baseline
            case RETWEET -> score += 10.0; // Slightly favor retweets for discovery
            case QUOTE_TWEET -> score += 15.0; // Quote tweets often add value
            case COMMENT -> score -= 10.0; // Comments less likely to be interesting to non-participants
        }
        
        return Math.max(0.0, Math.min(score, 100.0));
    }
    
    private double applyPenaltiesAndBonuses(Feed post, User user) {
        double multiplier = 1.0;
        
        // Penalty for very old posts
        if (!TimestampUtils.isRecent(post.getCreatedAt(), 72)) { // Older than 3 days
            multiplier *= 0.5;
        }
        
        // Bonus for posts with high engagement velocity
        if (TimestampUtils.isRecent(post.getCreatedAt(), 6) && post.getBaseEngagementScore() > 10.0) {
            multiplier *= 1.2;
        }
        
        // Penalty for posts with low engagement after some time
        if (!TimestampUtils.isRecent(post.getCreatedAt(), 24) && post.getBaseEngagementScore() < 2.0) {
            multiplier *= 0.3;
        }
        
        return multiplier;
    }
    
    private boolean hasCommonInterests(Feed post, User user) {
        // In a real system, you'd track user interests
        // For now, we'll use a simple hashtag matching approach
        Set<String> postHashtags = new HashSet<>(post.getHashtags());
        Set<String> userInterests = getUserInterests(user);
        
        postHashtags.retainAll(userInterests);
        return !postHashtags.isEmpty();
    }
    
    private List<String> getCommonInterests(Feed post, User user) {
        Set<String> postHashtags = new HashSet<>(post.getHashtags());
        Set<String> userInterests = getUserInterests(user);
        
        postHashtags.retainAll(userInterests);
        return new ArrayList<>(postHashtags);
    }
    
    private Set<String> getUserInterests(User user) {
        // In a real system, this would be based on user's interaction history
        // For now, return some sample interests
        return new HashSet<>(Arrays.asList("tech", "startup", "ai", "programming", "news"));
    }
    
    private boolean isTrendingHashtag(String hashtag) {
        // In a real system, this would check against trending hashtags
        // For now, simulate with some common hashtags
        Set<String> trending = Set.of("tech", "ai", "startup", "news", "breaking", "viral");
        return trending.contains(hashtag.toLowerCase());
    }
    
}