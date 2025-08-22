package io.shrouded.okara.repository;

import io.shrouded.okara.model.UserFeed;
import io.shrouded.okara.service.ReactiveFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserFeedRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "user_feeds";

    public Mono<UserFeed> save(UserFeed userFeed) {
        return firestoreService.save(COLLECTION_NAME, userFeed,
            userFeed::getId, (uf, id) -> uf.setId(id));
    }

    public Mono<UserFeed> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            UserFeed.class, (uf, docId) -> uf.setId(docId));
    }

    // Find user's feed (there's only one per user now)
    public Mono<UserFeed> findByUserId(String userId) {
        return firestoreService.findFirstByField(COLLECTION_NAME, "userId", userId, 
            UserFeed.class, (uf, docId) -> uf.setId(docId));
    }

    public Mono<Void> delete(UserFeed userFeed) {
        return firestoreService.delete(COLLECTION_NAME, userFeed, 
            userFeed::getId);
    }
}
