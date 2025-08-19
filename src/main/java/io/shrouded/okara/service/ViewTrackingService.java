package io.shrouded.okara.service;

import io.shrouded.okara.enums.ViewSource;
import io.shrouded.okara.model.User;
import io.shrouded.okara.model.ViewEvent;
import io.shrouded.okara.repository.UserRepository;
import io.shrouded.okara.repository.ViewEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ViewTrackingService {

    private final ViewEventRepository viewEventRepository;
    private final UserRepository userRepository;

    /**
     * Record a feed item view and update user's total view count
     */
    public Mono<Void> recordFeedItemView(String userId, String postId, String postAuthorId,
                                         ViewSource viewSource, Long viewDurationMs) {
        log.debug("Recording view: user={}, post={}, source={}", userId, postId, viewSource);

        // Create the view event
        ViewEvent viewEvent = new ViewEvent(userId, postId, postAuthorId, viewSource);
        if (viewDurationMs != null) {
            viewEvent.setViewDurationMs(viewDurationMs);
        }

        // Save view event and increment user's total view count
        return viewEventRepository.save(viewEvent)
                                  .flatMap(savedEvent -> incrementUserViewCount(userId))
                                  .then()
                                  .doOnSuccess(v -> log.debug("Successfully recorded view for user {} on post {}",
                                                              userId,
                                                              postId))
                                  .onErrorResume(e -> {
                                      log.error("Failed to record view for user {} on post {}: {}",
                                                userId,
                                                postId,
                                                e.getMessage());
                                      return Mono.empty(); // Don't fail the request if view tracking fails
                                  });
    }

    /**
     * Increment the user's total view count
     */
    private Mono<User> incrementUserViewCount(String userId) {
        return userRepository.findById(userId)
                             .flatMap(user -> {
                                 Integer currentCount = user.getTotalViewsCount() != null ? user.getTotalViewsCount() : 0;
                                 user.setTotalViewsCount(currentCount + 1);
                                 return userRepository.save(user);
                             })
                             .doOnSuccess(user -> log.debug("Incremented view count for user {} to {}",
                                                            userId,
                                                            user.getTotalViewsCount()));
    }

    /**
     * Check if user has already viewed a specific post (to avoid duplicate tracking)
     */
    public Mono<Boolean> hasUserViewedPost(String userId, String postId) {
        return viewEventRepository.findByUserIdAndPostId(userId, postId)
                                  .hasElements(); // Returns true if any views exist
    }

    /**
     * Get total views for a specific post
     */
    public Mono<Long> getPostViewCount(String postId) {
        return viewEventRepository.countByPostId(postId);
    }

    /**
     * Get total views by a specific user
     */
    public Mono<Long> getUserViewCount(String userId) {
        return viewEventRepository.countByUserId(userId);
    }
}