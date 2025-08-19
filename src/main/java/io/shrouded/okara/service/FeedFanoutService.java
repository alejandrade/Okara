package io.shrouded.okara.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.Timestamp;
import io.shrouded.okara.dto.event.FeedEvent;
import io.shrouded.okara.enums.UserFeedType;
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
        log.info("Processing POST_CREATED event for post {} by user {}", event.getPostId(), event.getAuthorId());

        return Mono.zip(
                           feedRepository.findById(event.getPostId())
                                         .switchIfEmpty(Mono.error(new RuntimeException("Post not found: " + event.getPostId()))),
                           userRepository.findById(event.getAuthorId())
                                         .switchIfEmpty(Mono.error(new RuntimeException("Author not found: " + event.getAuthorId())))
                   )
                   .flatMap(tuple -> {
                       Feed post = tuple.getT1();
                       User author = tuple.getT2();

                       // Simple fanout to author's followers
                       //return fanoutToFollowers(post, author);
                       return fanoutToDiscovery(post);
                   })
                   .onErrorResume(e -> {
                       log.error("Failed to handle POST_CREATED event: {}", e.getMessage());
                       return Mono.empty();
                   });
    }

    private Mono<Void> handlePostUpdated(FeedEvent event) {
        log.info("Processing POST_UPDATED event for post {}", event.getPostId());

        return feedRepository.findById(event.getPostId())
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found: " + event.getPostId())))
                             .flatMap(post -> updatePostInUserFeeds(post))
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

    private Mono<Void> fanoutToDiscovery(Feed post) {
        log.info("Fanning out post {} to discovery", post.getId());
        return userRepository.findAll()
                             .buffer(50)
                             .flatMap(userBatch -> processFanoutDiscoveryBatch(post, userBatch))
                             .then();
    }

    private Mono<Void> processFanoutDiscoveryBatch(Feed post, List<User> users) {
        log.debug("Processing discovery fanout batch of {} users for post {}", users.size(), post.getId());

        List<Mono<UserFeed>> feedOperations = new ArrayList<>();

        // Prepare all user feeds for this batch
        for (User user : users) {
            feedOperations.add(
                    getUserFeed(user.getId(), UserFeedType.PERSONAL)
                            .map(userFeed -> {
                                userFeed.addItem(new FeedItem(post));
                                return userFeed;
                            })
                            .onErrorResume(e -> {
                                log.error("Failed to prepare feed for user {}: {}", user.getId(), e.getMessage());
                                return Mono.empty();
                            })
            );
        }

        // Execute all feed preparations and then batch save
        return Flux.fromIterable(feedOperations)
                   .flatMap(operation -> operation)
                   .collectList()
                   .filter(feeds -> !feeds.isEmpty())
                   .flatMap(feeds -> userFeedRepository.saveAll(feeds).then())
                   .onErrorResume(e -> {
                       log.error("Failed to batch save feeds for discovery fanout: {}", e.getMessage());
                       return Mono.empty();
                   });
    }

    private Mono<Void> fanoutToFollowers(Feed post, User author) {
        log.info("Fanning out post {} to {} followers", post.getId(), author.getFollowersCount());

        List<String> followers = author.getFollowers() != null
                ? new ArrayList<>(author.getFollowers())
                : new ArrayList<>();

        // Always include the author themself so their posts appear in their own feed
        if (!followers.contains(author.getId())) {
            followers.add(author.getId());
        }

        // Process followers in batches to avoid overwhelming the system
        int batchSize = 100;
        List<Mono<Void>> batchOperations = new ArrayList<>();

        for (int i = 0; i < followers.size(); i += batchSize) {
            int endIndex = Math.min(i + batchSize, followers.size());
            List<String> batch = followers.subList(i, endIndex);

            // Process batch
            batchOperations.add(processFanoutBatch(post, batch));
        }

        return Flux.fromIterable(batchOperations)
                   .flatMap(operation -> operation)
                   .then();
    }

    private Mono<Void> processFanoutBatch(Feed post, List<String> followerIds) {
        List<Mono<Void>> operations = new ArrayList<>();

        for (String followerId : followerIds) {
            operations.add(
                    userRepository.findById(followerId)
                                  .flatMap(follower -> {
                                      // Create simple feed item without algorithmic scoring
                                      FeedItem feedItem = new FeedItem(post);

                                      // Add to personal feed only
                                      return addToUserFeed(followerId, UserFeedType.PERSONAL, feedItem);
                                  })
                                  .onErrorResume(e -> {
                                      log.error("Failed to add post {} to follower {} feed: {}",
                                                post.getId(),
                                                followerId,
                                                e.getMessage());
                                      return Mono.empty();
                                  })
            );
        }

        return Flux.fromIterable(operations)
                   .flatMap(operation -> operation)
                   .then();
    }

    private Mono<Void> addToUserFeed(String userId, UserFeedType feedType, FeedItem feedItem) {
        return getUserFeed(userId, feedType)
                .flatMap(userFeed -> {
                    // Add the item
                    userFeed.addItem(feedItem);

                    // Save
                    return userFeedRepository.save(userFeed);
                })
                .then()
                .onErrorResume(e -> {
                    log.error("Failed to add item to user {} feed {}: {}", userId, feedType, e.getMessage());
                    return Mono.empty();
                });
    }

    private Mono<UserFeed> getUserFeed(String userId, UserFeedType feedType) {
        return userFeedRepository.findByUserIdAndFeedType(userId, feedType)
                                 .switchIfEmpty(Mono.defer(() -> {
                                     // Create new feed
                                     return Mono.just(new UserFeed(userId, feedType));
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
                       User follower = tuple.getT2();

                       if (recentPosts.isEmpty()) {
                           return Mono.empty();
                       }

                       // Create simple feed items without algorithmic scoring
                       List<FeedItem> feedItems = new ArrayList<>();
                       for (Feed post : recentPosts) {
                           FeedItem feedItem = new FeedItem(post);
                           feedItem.setReasonShown("From " + post.getAuthorUsername() + " (recently followed)");
                           feedItems.add(feedItem);
                       }

                       // Add to personal feed only
                       return getUserFeed(followerId, UserFeedType.PERSONAL)
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
                                 .collectList()
                                 .flatMap(userFeeds -> {
                                     List<Mono<UserFeed>> saveOperations = new ArrayList<>();

                                     for (UserFeed feed : userFeeds) {
                                         boolean modified = feed.getItems()
                                                                .removeIf(item -> unfollowedId.equals(item.getAuthorId()));

                                         if (modified) {
                                             feed.setTotalItems(feed.getItems().size());
                                             feed.setLastUpdated(Timestamp.now());
                                             saveOperations.add(userFeedRepository.save(feed));
                                         }
                                     }

                                     if (saveOperations.isEmpty()) {
                                         return Mono.empty();
                                     }

                                     return Flux.fromIterable(saveOperations)
                                                .flatMap(operation -> operation)
                                                .then();
                                 })
                                 .onErrorResume(e -> {
                                     log.error("Failed to remove posts from user {} feed: {}",
                                               followerId,
                                               e.getMessage());
                                     return Mono.empty();
                                 });
    }
}