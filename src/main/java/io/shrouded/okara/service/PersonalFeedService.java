package io.shrouded.okara.service;

import io.shrouded.okara.enums.UserFeedType;
import io.shrouded.okara.model.FeedItem;
import io.shrouded.okara.model.UserFeed;
import io.shrouded.okara.repository.UserFeedRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

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
    public Mono<List<FeedItem>> getPersonalFeed(String userId, int limit, String sinceId) {
        return getUserFeedItems(userId, UserFeedType.PERSONAL, limit, sinceId);
    }

    /**
     * Initialize personal feed for a new user
     */
    public Mono<Void> initializeUserFeed(String userId) {
        log.info("Initializing personal feed for new user: {}", userId);

        // Create single personal feed
        UserFeed personalFeed = new UserFeed(userId, UserFeedType.PERSONAL);
        return userFeedRepository.save(personalFeed)
                                 .then()
                                 .doOnSuccess(v -> log.info("Successfully initialized personal feed for user: {}",
                                                            userId))
                                 .onErrorResume(e -> {
                                     log.error("Failed to initialize personal feed for user {}: {}",
                                               userId,
                                               e.getMessage(),
                                               e);
                                     return Mono.error(new RuntimeException("Failed to initialize user feed", e));
                                 });
    }


    private Mono<List<FeedItem>> getUserFeedItems(String userId, UserFeedType feedType, int limit, String sinceId) {
        return userFeedRepository.findByUserIdAndFeedType(userId, feedType)
                                 .map(userFeed -> {
                                     List<FeedItem> items = userFeed.getItemsSince(sinceId, limit);

                                     log.debug("Retrieved {} items from {} feed for user {} (limit {}, sinceId {})",
                                               items.size(), feedType, userId, limit, sinceId);

                                     return items;
                                 })
                                 .switchIfEmpty(Mono.fromCallable(() -> {
                                     log.warn("No feed found for user {} and type {}", userId, feedType);
                                     return new ArrayList<FeedItem>();
                                 }))
                                 .onErrorResume(e -> {
                                     log.error("Failed to get {} feed for user {}: {}",
                                               feedType,
                                               userId,
                                               e.getMessage());
                                     return Mono.just(new ArrayList<FeedItem>());
                                 });
    }
}