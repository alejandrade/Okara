package io.shrouded.okara.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import io.shrouded.okara.dto.event.FeedEvent;
import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.FeedItem;
import io.shrouded.okara.model.User;
import io.shrouded.okara.model.UserFeed;
import io.shrouded.okara.repository.FeedRepository;
import io.shrouded.okara.repository.UserFeedRepository;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedFanoutService {

    private final UserFeedRepository userFeedRepository;
    private final UserRepository userRepository;
    private final FeedRepository feedRepository;
    private final ObjectMapper objectMapper;

    public Mono<Void> processFeedEvent(String eventJson) {
        try {
            FeedEvent event = objectMapper.readValue(eventJson, FeedEvent.class);

            return switch (event.getEventType()) {
                case POST_CREATED -> handlePostCreated(event);
                case POST_UPDATED -> handlePostUpdated(event);
                case POST_DELETED -> handlePostDeleted(event);
                case USER_FOLLOWED -> handleUserFollowed(event);
                case USER_UNFOLLOWED -> handleUserUnfollowed(event);
            };

        } catch (Exception e) {
            log.error("Failed to process feed event: {}", e.getMessage(), e);
            return Mono.error(new RuntimeException("Feed event processing failed", e));
        }
    }

    private Mono<Void> handlePostCreated(FeedEvent event) {
        log.info("Processing POST_CREATED event for post {} by user {} to chatrooms {}", 
                event.getPostId(), event.getAuthorId(), event.getChatroomIds());

        // Chatrooms are required for post distribution
        if (event.getChatroomIds() == null || event.getChatroomIds().isEmpty()) {
            log.error("No chatrooms specified for post {}, cannot distribute", event.getPostId());
            return Mono.error(new RuntimeException("Chatrooms are required for post distribution"));
        }

        return feedRepository.findById(event.getPostId())
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found: " + event.getPostId())))
                             .flatMap(post -> fanoutToChatrooms(post, event.getChatroomIds()))
                             .onErrorResume(e -> {
                                 log.error("Failed to handle POST_CREATED event: {}", e.getMessage());
                                 return Mono.empty();
                             });
    }

    private Mono<Void> handlePostUpdated(FeedEvent event) {
        log.info("Processing POST_UPDATED event for post {}", event.getPostId());

        return feedRepository.findById(event.getPostId())
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found: " + event.getPostId())))
                             .flatMap(this::updatePostInUserFeeds)
                             .onErrorResume(e -> {
                                 log.error("Failed to handle POST_UPDATED event: {}", e.getMessage());
                                 return Mono.empty();
                             });
    }

    private Mono<Void> handlePostDeleted(FeedEvent event) {
        log.info("Processing POST_DELETED event for post {}", event.getPostId());

        return removePostFromUserFeeds(event.getPostId())
                .onErrorResume(e -> {
                    log.error("Failed to handle POST_DELETED event: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> handleUserFollowed(FeedEvent event) {
        String followerId = event.getMetadata().followerId();
        String followedId = event.getMetadata().followedId();

        log.info("Processing USER_FOLLOWED event: {} followed {}", followerId, followedId);

        return backfillUserFeed(followerId, followedId)
                .onErrorResume(e -> {
                    log.error("Failed to handle USER_FOLLOWED event: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> handleUserUnfollowed(FeedEvent event) {
        String followerId = event.getMetadata().followerId();
        String unfollowedId = event.getMetadata().unfollowedId();

        log.info("Processing USER_UNFOLLOWED event: {} unfollowed {}", followerId, unfollowedId);

        return removeUserPostsFromFeed(followerId, unfollowedId)
                .onErrorResume(e -> {
                    log.error("Failed to handle USER_UNFOLLOWED event: {}", e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<Void> fanoutToChatrooms(Feed post, List<String> chatroomIds) {
        log.info("Fanning out post {} to chatrooms {}", post.getId(), chatroomIds);
        
        // Find all users who are members of any of the specified chatrooms
        return userRepository.findAll()
                             .filter(user -> user.getChatrooms() != null && 
                                           user.getChatrooms().stream()
                                                              .anyMatch(uc -> chatroomIds.contains(uc.getChatroomId())))
                             .buffer(50)
                             .flatMap(userBatch -> processFanoutChatroomBatch(post, userBatch, chatroomIds))
                             .then();
    }

    private Mono<Void> processFanoutChatroomBatch(Feed post, List<User> users, List<String> chatroomIds) {
        log.debug("Processing chatroom fanout batch of {} users for post {} to chatrooms {}", 
                 users.size(), post.getId(), chatroomIds);

        List<Mono<UserFeed>> userFeedUpdates = new ArrayList<>();
        
        for (User user : users) {
            // For each chatroom this user is in that the post was sent to
            for (String chatroomId : chatroomIds) {
                boolean userInChatroom = user.getChatrooms().stream()
                                            .anyMatch(uc -> uc.getChatroomId().equals(chatroomId));
                
                if (userInChatroom) {
                    // Create FeedItem for this specific chatroom
                    FeedItem feedItem = new FeedItem(post, chatroomId);
                    feedItem.setReasonShown("From chatroom");
                    
                    // Find or create user's feed and add the item
                    Mono<UserFeed> userFeedUpdate = userFeedRepository.findByUserId(user.getId())
                            .switchIfEmpty(Mono.fromCallable(() -> new UserFeed(user.getId())))
                            .map(userFeed -> {
                                userFeed.addItem(feedItem);
                                return userFeed;
                            })
                            .flatMap(userFeedRepository::save);
                    
                    userFeedUpdates.add(userFeedUpdate);
                    break; // Only add once per user, even if they're in multiple target chatrooms
                }
            }
        }

        return Mono.when(userFeedUpdates)
                   .doOnSuccess(v -> log.debug("Successfully updated {} user feeds for chatroom fanout", userFeedUpdates.size()));
    }

    private Mono<UserFeed> getUserFeed(String userId) {
        return userFeedRepository.findByUserId(userId)
                                 .switchIfEmpty(Mono.defer(() -> {
                                     // Create new feed
                                     return Mono.just(new UserFeed(userId));
                                 }));
    }

    private Mono<Void> updatePostInUserFeeds(Feed post) {
        // This would be complex - you'd need to find all user feeds containing this post
        // and update the cached data. For now, we'll log it.
        log.info("Updating post {} in user feeds (not implemented)", post.getId());
        return Mono.empty();
    }

    private Mono<Void> removePostFromUserFeeds(String postId) {
        // This would require finding all user feeds containing this post and removing it
        // For now, we'll log it.
        log.info("Removing post {} from user feeds (not implemented)", postId);
        return Mono.empty();
    }

    private Mono<Void> backfillUserFeed(String followerId, String followedId) {
        log.info("Backfilling feed for user {} with recent posts from {}", followerId, followedId);

        return Mono.zip(
                           feedRepository.findByAuthorIdAndParentIdIsNull(followedId)
                                         .take(50) // Limit to recent posts
                                         .collectList(),
                           userRepository.findById(followerId)
                   )
                   .flatMap(tuple -> {
                       List<Feed> recentPosts = tuple.getT1();

                       if (recentPosts.isEmpty()) {
                           return Mono.empty();
                       }

                       // Create simple feed items without algorithmic scoring
                       List<FeedItem> feedItems = new ArrayList<>();
                       for (Feed post : recentPosts) {
                           FeedItem feedItem = new FeedItem(post);
                           feedItem.setReasonShown("From " + post.getAuthorDisplayName() + " (recently followed)");
                           feedItems.add(feedItem);
                       }

                       // Add to personal feed only
                       return getUserFeed(followerId)
                               .flatMap(personalFeed -> {
                                   personalFeed.addItems(feedItems);
                                   return userFeedRepository.save(personalFeed);
                               })
                               .then();
                   })
                   .then()
                   .onErrorResume(e -> {
                       log.error("Failed to backfill feed for user {}: {}", followerId, e.getMessage());
                       return Mono.empty();
                   });
    }

    private Mono<Void> removeUserPostsFromFeed(String followerId, String unfollowedId) {
        log.info("Removing posts from user {} feed for unfollowed user {}", followerId, unfollowedId);

        return userFeedRepository.findByUserId(followerId)
                                 .flatMap(userFeed -> {
                                     boolean modified = userFeed.getItems()
                                                               .removeIf(item -> unfollowedId.equals(item.getAuthorId()));

                                     if (modified) {
                                         userFeed.setTotalItems(userFeed.getItems().size());
                                         userFeed.setLastUpdated(Timestamp.now());
                                         return userFeedRepository.save(userFeed).then();
                                     } else {
                                         return Mono.empty();
                                     }
                                 })
                                 .onErrorResume(e -> {
                                     log.error("Failed to remove posts from user {} feed: {}",
                                               followerId,
                                               e.getMessage());
                                     return Mono.empty();
                                 });
    }
}