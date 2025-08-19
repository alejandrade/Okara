package io.shrouded.okara.service;

import io.shrouded.okara.model.UserFeed;
import io.shrouded.okara.repository.UserFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import com.google.cloud.Timestamp;
import io.shrouded.okara.util.TimestampUtils;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PersonalFeedService {
    
    private final UserFeedRepository userFeedRepository;
    
    /**
     * Get user's personal feed (simple chronological for now)
     */
    public Mono<List<UserFeed.FeedItem>> getPersonalFeed(String userId, int limit, String sinceId) {
        return getUserFeedItems(userId, UserFeed.FeedType.PERSONAL, limit, sinceId);
    }
    
    /**
     * Initialize personal feed for a new user
     */
    public Mono<Void> initializeUserFeed(String userId) {
        log.info("Initializing personal feed for new user: {}", userId);
        
        // Create single personal feed
        UserFeed personalFeed = new UserFeed(userId, UserFeed.FeedType.PERSONAL);
        return userFeedRepository.save(personalFeed)
            .then()
            .doOnSuccess(v -> log.info("Successfully initialized personal feed for user: {}", userId))
            .onErrorResume(e -> {
                log.error("Failed to initialize personal feed for user {}: {}", userId, e.getMessage(), e);
                return Mono.error(new RuntimeException("Failed to initialize user feed", e));
            });
    }
    
    /**
     * Clean up user feed (for user deletion)
     */
    public Mono<Void> deleteUserFeed(String userId) {
        log.info("Deleting personal feed for user: {}", userId);
        
        return userFeedRepository.deleteByUserId(userId)
            .doOnSuccess(v -> log.info("Successfully deleted personal feed for user: {}", userId))
            .onErrorResume(e -> {
                log.error("Failed to delete personal feed for user {}: {}", userId, e.getMessage(), e);
                return Mono.error(new RuntimeException("Failed to delete user feed", e));
            });
    }
    
    /**
     * Get feed statistics for a user
     */
    public Mono<FeedStats> getUserFeedStats(String userId) {
        return userFeedRepository.findByUserIdAndFeedType(userId, UserFeed.FeedType.PERSONAL)
            .map(userFeed -> {
                FeedStats stats = new FeedStats();
                stats.totalPosts = userFeed.getTotalItems();
                stats.lastUpdate = userFeed.getLastUpdated();
                return stats;
            })
            .switchIfEmpty(Mono.just(new FeedStats()))
            .onErrorResume(e -> {
                log.error("Failed to get feed stats for user {}: {}", userId, e.getMessage());
                return Mono.just(new FeedStats());
            });
    }
    
    /**
     * Add a post to user's personal feed (called by pub/sub handler)
     */
    public Mono<Void> addPostToPersonalFeed(String userId, UserFeed.FeedItem feedItem) {
        log.info("Adding post {} to personal feed for user {}", feedItem.getPostId(), userId);
        
        return userFeedRepository.findByUserIdAndFeedType(userId, UserFeed.FeedType.PERSONAL)
            .switchIfEmpty(Mono.defer(() -> {
                // No feed exists, create one
                log.info("No personal feed found for user {}, creating one", userId);
                return Mono.just(new UserFeed(userId, UserFeed.FeedType.PERSONAL));
            }))
            .flatMap(feed -> {
                feed.addItem(feedItem);
                return userFeedRepository.save(feed);
            })
            .then()
            .doOnSuccess(v -> log.info("Successfully added post to personal feed for user {}", userId))
            .onErrorResume(e -> {
                log.error("Failed to add post to personal feed for user {}: {}", userId, e.getMessage(), e);
                return Mono.empty(); // Don't fail the entire process if one user's feed fails
            });
    }
    
    /**
     * Mark feed items as read/seen
     */
    public Mono<Void> markFeedItemsSeen(String userId, UserFeed.FeedType feedType, List<String> postIds) {
        // In a full implementation, you'd track which posts the user has seen
        // This could be used for analytics and to avoid showing the same content
        log.info("Marking {} items as seen for user {} in {} feed", 
            postIds.size(), userId, feedType);
        
        return Mono.empty();
    }
    
    private Mono<List<UserFeed.FeedItem>> getUserFeedItems(String userId, UserFeed.FeedType feedType, int limit, String sinceId) {
        return userFeedRepository.findByUserIdAndFeedType(userId, feedType)
            .map(userFeed -> {
                List<UserFeed.FeedItem> items = userFeed.getItemsSince(sinceId, limit);
                
                log.debug("Retrieved {} items from {} feed for user {} (limit {}, sinceId {})", 
                    items.size(), feedType, userId, limit, sinceId);
                
                return items;
            })
            .switchIfEmpty(Mono.fromCallable(() -> {
                log.warn("No feed found for user {} and type {}", userId, feedType);
                return new ArrayList<UserFeed.FeedItem>();
            }))
            .onErrorResume(e -> {
                log.error("Failed to get {} feed for user {}: {}", feedType, userId, e.getMessage());
                return Mono.just(new ArrayList<UserFeed.FeedItem>());
            });
    }
    
    /**
     * Statistics about a user's personal feed
     */
    public static class FeedStats {
        public int totalPosts = 0;
        public Timestamp lastUpdate = Timestamp.MIN_VALUE;
        
        public int totalPosts() {
            return totalPosts;
        }
        
        public int totalLikes() {
            return 0; // Placeholder for now
        }
        
        public int totalComments() {
            return 0; // Placeholder for now
        }
        
        public int totalRetweets() {
            return 0; // Placeholder for now
        }
        
        public Timestamp getLastUpdate() {
            return lastUpdate;
        }
    }
}