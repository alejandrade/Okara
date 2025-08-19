package io.shrouded.okara.service;

import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.UserFeed;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@Slf4j
public class FeedMappingService {
    
    /**
     * Convert UserFeed.FeedItem to Feed for frontend compatibility
     */
    public Feed convertToFeed(UserFeed.FeedItem feedItem) {
        try {
            Feed feed = new Feed();
            
            // Basic properties
            feed.setId(feedItem.getPostId());
            feed.setAuthorId(feedItem.getAuthorId());
            feed.setAuthorUsername(feedItem.getAuthorUsername());
            feed.setAuthorDisplayName(feedItem.getAuthorDisplayName());
            feed.setAuthorProfileImageUrl(feedItem.getAuthorProfileImageUrl());
            
            // Content
            feed.setContent(feedItem.getContent());
            feed.setImageUrls(feedItem.getImageUrls() != null ? feedItem.getImageUrls() : new ArrayList<>());
            feed.setVideoUrl(feedItem.getVideoUrl());
            
            // Type and timing
            feed.setType(feedItem.getPostType());
            feed.setCreatedAt(feedItem.getCreatedAt());
            feed.setUpdatedAt(feedItem.getCreatedAt()); // Use created time as fallback
            
            // Engagement data (cached in FeedItem for performance)
            feed.setLikesCount(feedItem.getLikesCount());
            feed.setRetweetsCount(feedItem.getRetweetsCount());
            feed.setCommentsCount(feedItem.getCommentsCount());
            feed.setViewsCount(0); // Default value
            
            // Initialize empty lists for now (would need separate queries for full data)
            feed.setLikedBy(new ArrayList<>());
            feed.setDislikedBy(new ArrayList<>());
            feed.setRetweetedBy(new ArrayList<>());
            
            // Retweet/Quote data
            feed.setOriginalPostId(feedItem.getOriginalPostId());
            feed.setQuoteTweetComment(feedItem.getRetweetComment());
            
            // Default values
            feed.setParentId(null);
            feed.setRootId(null);
            feed.setPinned(false);
            feed.setHashtags(new ArrayList<>());
            feed.setMentions(new ArrayList<>());
            feed.setDislikesCount(0);
            feed.setBaseEngagementScore(feedItem.getAlgorithmScore());
            
            return feed;
            
        } catch (Exception e) {
            log.error("Error converting FeedItem to Feed: {}", e.getMessage(), e);
            return null;
        }
    }
    
    /**
     * Convert list of UserFeed.FeedItems to Feed list
     */
    public List<Feed> convertToFeedList(List<UserFeed.FeedItem> feedItems) {
        return feedItems.stream()
            .map(this::convertToFeed)
            .filter(Objects::nonNull)
            .collect(Collectors.toList());
    }
}