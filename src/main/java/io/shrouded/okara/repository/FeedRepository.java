package io.shrouded.okara.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import io.shrouded.okara.model.Feed;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface FeedRepository extends FirestoreReactiveRepository<Feed> {

    // Main feed posts (no parent)
    Flux<Feed> findByParentIdIsNull();

    // User's posts (no parent)
    Flux<Feed> findByAuthorIdAndParentIdIsNull(String authorId);

    // Comments for a post
    Flux<Feed> findByParentId(String parentId);

    // All comments in a thread (by root)
    Flux<Feed> findByRootId(String rootId);

    // Count user's posts (no parent)
    Mono<Long> countByAuthorIdAndParentIdIsNull(String authorId);
}
