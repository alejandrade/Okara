package io.shrouded.okara.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import io.shrouded.okara.model.ViewEvent;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ViewEventRepository extends FirestoreReactiveRepository<ViewEvent> {

    // Find all views by a specific user
    Flux<ViewEvent> findByUserId(String userId);

    // Find all views for a specific post
    Flux<ViewEvent> findByPostId(String postId);

    // Find views by user and post (to check if already viewed)
    Flux<ViewEvent> findByUserIdAndPostId(String userId, String postId);

    // Count total views by user
    Mono<Long> countByUserId(String userId);

    // Count total views for a post
    Mono<Long> countByPostId(String postId);

    // Find views by view source (for analytics)
    Flux<ViewEvent> findByViewSource(ViewEvent.ViewSource viewSource);

    // Find recent views by user (for recent activity)
    Flux<ViewEvent> findByUserIdOrderByViewedAtDesc(String userId);
}