package io.shrouded.okara.repository;

import com.google.cloud.firestore.Query;
import io.shrouded.okara.model.Feed;
import io.shrouded.okara.service.ReactiveFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Repository
@RequiredArgsConstructor
@Slf4j
public class FeedRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "feeds";

    public Mono<Feed> save(Feed feed) {
        return firestoreService.save(COLLECTION_NAME, feed, 
            feed::getId, (f, id) -> f.setId(id));
    }

    public Mono<Feed> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            Feed.class, (f, docId) -> f.setId(docId));
    }

    // User's posts (no parent)
    public Flux<Feed> findByAuthorIdAndParentIdIsNull(String authorId) {
        return firestoreService.findByTwoFieldsOrderBy(COLLECTION_NAME, 
            "authorId", authorId, "parentId", null, 
            "createdAt", Query.Direction.DESCENDING, Feed.class, (f, docId) -> f.setId(docId));
    }

    // Comments for a post
    public Flux<Feed> findByParentId(String parentId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "parentId", parentId, 
            "createdAt", Query.Direction.ASCENDING, Feed.class, (f, docId) -> f.setId(docId));
    }

    // Find feeds containing specific chatroom ID
    public Flux<Feed> findByChatroomIdsContaining(String chatroomId) {
        return firestoreService.findByArrayContainsOrderBy(COLLECTION_NAME, "chatroomIds", chatroomId, 
            "createdAt", Query.Direction.DESCENDING, Feed.class, (f, docId) -> f.setId(docId));
    }


    public Mono<Void> deleteById(String id) {
        return firestoreService.deleteById(COLLECTION_NAME, id);
    }

    public Mono<Void> delete(Feed feed) {
        return firestoreService.delete(COLLECTION_NAME, feed, 
            feed::getId);
    }
}
