package io.shrouded.okara.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import io.shrouded.okara.enums.UserFeedType;
import io.shrouded.okara.model.UserFeed;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface UserFeedRepository extends FirestoreReactiveRepository<UserFeed> {

    // Find user's feed by type  
    Mono<UserFeed> findByUserIdAndFeedType(String userId, UserFeedType feedType);

    // Get all feeds for a user (all types)
    Flux<UserFeed> findByUserId(String userId);

    // Delete all feeds for a user (choose one of these signatures)
    Mono<Void> deleteByUserId(String userId);
    // or, if you want the number of rows deleted:
    // Mono<Long> deleteByUserId(String userId);
}
