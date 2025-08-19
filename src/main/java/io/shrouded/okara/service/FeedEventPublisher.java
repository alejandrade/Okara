package io.shrouded.okara.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shrouded.okara.model.Feed;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedEventPublisher {
    
    @Qualifier("feedEventsOutputChannel")
    private final MessageChannel feedEventsOutputChannel;
    
    private final ObjectMapper objectMapper;
    
    public void publishPostCreated(Feed post) {
        try {
            FeedEvent event = FeedEvent.builder()
                .eventType(FeedEventType.POST_CREATED)
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthorUsername())
                .content(post.getContent())
                .createdAt(post.getCreatedAt().toString())
                .metadata(createMetadata(post))
                .build();
            
            publishEvent(event);
            log.info("Published POST_CREATED event for post {} by user {}", post.getId(), post.getAuthorId());
            
        } catch (Exception e) {
            log.error("Failed to publish POST_CREATED event for post {}: {}", post.getId(), e.getMessage());
            throw new RuntimeException("Failed to publish feed event", e);
        }
    }
    
    public void publishPostUpdated(Feed post) {
        try {
            FeedEvent event = FeedEvent.builder()
                .eventType(FeedEventType.POST_UPDATED)
                .postId(post.getId())
                .authorId(post.getAuthorId())
                .authorUsername(post.getAuthorUsername())
                .content(post.getContent())
                .createdAt(post.getCreatedAt().toString())
                .metadata(createMetadata(post))
                .build();
            
            publishEvent(event);
            log.info("Published POST_UPDATED event for post {} by user {}", post.getId(), post.getAuthorId());
            
        } catch (Exception e) {
            log.error("Failed to publish POST_UPDATED event for post {}: {}", post.getId(), e.getMessage());
            throw new RuntimeException("Failed to publish feed event", e);
        }
    }
    
    public void publishPostDeleted(String postId, String authorId) {
        try {
            FeedEvent event = FeedEvent.builder()
                .eventType(FeedEventType.POST_DELETED)
                .postId(postId)
                .authorId(authorId)
                .build();
            
            publishEvent(event);
            log.info("Published POST_DELETED event for post {} by user {}", postId, authorId);
            
        } catch (Exception e) {
            log.error("Failed to publish POST_DELETED event for post {}: {}", postId, e.getMessage());
            throw new RuntimeException("Failed to publish feed event", e);
        }
    }
    
    public void publishUserFollowed(String followerId, String followedId) {
        try {
            FeedEvent event = FeedEvent.builder()
                .eventType(FeedEventType.USER_FOLLOWED)
                .authorId(followedId) // The person being followed
                .metadata(Map.of("followerId", followerId, "followedId", followedId))
                .build();
            
            publishEvent(event);
            log.info("Published USER_FOLLOWED event: {} followed {}", followerId, followedId);
            
        } catch (Exception e) {
            log.error("Failed to publish USER_FOLLOWED event: {}", e.getMessage());
            throw new RuntimeException("Failed to publish feed event", e);
        }
    }
    
    public void publishUserUnfollowed(String followerId, String unfollowedId) {
        try {
            FeedEvent event = FeedEvent.builder()
                .eventType(FeedEventType.USER_UNFOLLOWED)
                .authorId(unfollowedId) // The person being unfollowed
                .metadata(Map.of("followerId", followerId, "unfollowedId", unfollowedId))
                .build();
            
            publishEvent(event);
            log.info("Published USER_UNFOLLOWED event: {} unfollowed {}", followerId, unfollowedId);
            
        } catch (Exception e) {
            log.error("Failed to publish USER_UNFOLLOWED event: {}", e.getMessage());
            throw new RuntimeException("Failed to publish feed event", e);
        }
    }
    
    private void publishEvent(FeedEvent event) throws Exception {
        String eventJson = objectMapper.writeValueAsString(event);
        
        feedEventsOutputChannel.send(
            MessageBuilder.withPayload(eventJson)
                .setHeader("eventType", event.getEventType().name())
                .setHeader("postId", event.getPostId())
                .setHeader("authorId", event.getAuthorId())
                .build()
        );
    }
    
    private Map<String, Object> createMetadata(Feed post) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("postType", post.getType().name());
        metadata.put("likesCount", post.getLikesCount());
        metadata.put("retweetsCount", post.getRetweetsCount());
        metadata.put("commentsCount", post.getCommentsCount());
        metadata.put("baseEngagementScore", post.getBaseEngagementScore());
        metadata.put("hashtags", post.getHashtags());
        metadata.put("mentions", post.getMentions());
        
        if (post.getImageUrls() != null && !post.getImageUrls().isEmpty()) {
            metadata.put("hasImages", true);
            metadata.put("imageCount", post.getImageUrls().size());
        }
        
        if (post.getVideoUrl() != null) {
            metadata.put("hasVideo", true);
        }
        
        if (post.getParentId() != null) {
            metadata.put("isReply", true);
            metadata.put("parentId", post.getParentId());
        }
        
        return metadata;
    }
    
    // Event types for feed processing
    public enum FeedEventType {
        POST_CREATED,
        POST_UPDATED,
        POST_DELETED,
        USER_FOLLOWED,
        USER_UNFOLLOWED
    }
    
    // Event data structure
    @Getter
    public static class FeedEvent {
        // Getters
        private FeedEventType eventType;
        private String postId;
        private String authorId;
        private String authorUsername;
        private String content;
        private String createdAt;
        private Map<String, Object> metadata;
        
        // Builder pattern
        public static FeedEventBuilder builder() {
            return new FeedEventBuilder();
        }
        
        public static class FeedEventBuilder {
            private final FeedEvent event = new FeedEvent();
            
            public FeedEventBuilder eventType(FeedEventType eventType) {
                event.eventType = eventType;
                return this;
            }
            
            public FeedEventBuilder postId(String postId) {
                event.postId = postId;
                return this;
            }
            
            public FeedEventBuilder authorId(String authorId) {
                event.authorId = authorId;
                return this;
            }
            
            public FeedEventBuilder authorUsername(String authorUsername) {
                event.authorUsername = authorUsername;
                return this;
            }
            
            public FeedEventBuilder content(String content) {
                event.content = content;
                return this;
            }
            
            public FeedEventBuilder createdAt(String createdAt) {
                event.createdAt = createdAt;
                return this;
            }
            
            public FeedEventBuilder metadata(Map<String, Object> metadata) {
                event.metadata = metadata;
                return this;
            }
            
            public FeedEvent build() {
                return event;
            }
        }

    }
}