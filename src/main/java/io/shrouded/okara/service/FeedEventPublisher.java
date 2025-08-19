package io.shrouded.okara.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.shrouded.okara.dto.event.FeedEvent;
import io.shrouded.okara.dto.event.FeedEventMetadata;
import io.shrouded.okara.enums.FeedEventType;
import io.shrouded.okara.model.Feed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

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
                                       .metadata(FeedEventMetadata.forFollow(followerId, followedId))
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
                                       .metadata(FeedEventMetadata.forUnfollow(followerId, unfollowedId))
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

    private FeedEventMetadata createMetadata(Feed post) {
        return FeedEventMetadata.forPost(
            post.getType(),
            post.getLikesCount(),
            post.getRetweetsCount(),
            post.getCommentsCount(),
            post.getBaseEngagementScore(),
            post.getHashtags(),
            post.getMentions(),
            post.getImageUrls() != null && !post.getImageUrls().isEmpty(),
            post.getImageUrls() != null ? post.getImageUrls().size() : null,
            post.getVideoUrl() != null,
            post.getParentId() != null,
            post.getParentId()
        );
    }
}