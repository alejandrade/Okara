package io.shrouded.okara.repository;

import com.google.cloud.firestore.Query;
import io.shrouded.okara.enums.ViewSource;
import io.shrouded.okara.model.ViewEvent;
import io.shrouded.okara.service.ReactiveFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class ViewEventRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "view_events";

    public Mono<ViewEvent> save(ViewEvent viewEvent) {
        return firestoreService.save(COLLECTION_NAME, viewEvent, 
            viewEvent.getId(), (ve, id) -> ve.setId(id));
    }

    public Mono<ViewEvent> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    // Find all views by a specific user
    public Flux<ViewEvent> findByUserId(String userId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "userId", userId, 
            "viewedAt", Query.Direction.DESCENDING, ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    // Find all views for a specific post
    public Flux<ViewEvent> findByPostId(String postId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "postId", postId, 
            "viewedAt", Query.Direction.DESCENDING, ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    // Find views by user and post (to check if already viewed)
    public Flux<ViewEvent> findByUserIdAndPostId(String userId, String postId) {
        return firestoreService.findByTwoFields(COLLECTION_NAME, 
            "userId", userId, "postId", postId, 
            ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    // Count total views by user
    public Mono<Long> countByUserId(String userId) {
        return firestoreService.countByField(COLLECTION_NAME, "userId", userId);
    }

    // Count total views for a post
    public Mono<Long> countByPostId(String postId) {
        return firestoreService.countByField(COLLECTION_NAME, "postId", postId);
    }

    // Find views by view source (for analytics)
    public Flux<ViewEvent> findByViewSource(ViewSource viewSource) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "viewSource", viewSource.name(), 
            "viewedAt", Query.Direction.DESCENDING, ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    // Find recent views by user (for recent activity)
    public Flux<ViewEvent> findByUserIdOrderByViewedAtDesc(String userId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "userId", userId, 
            "viewedAt", Query.Direction.DESCENDING, ViewEvent.class, (ve, docId) -> ve.setId(docId));
    }

    public Mono<Void> delete(ViewEvent viewEvent) {
        return firestoreService.deleteById(COLLECTION_NAME, viewEvent.getId());
    }
}