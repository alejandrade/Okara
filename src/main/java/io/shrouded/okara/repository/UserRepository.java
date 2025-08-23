package io.shrouded.okara.repository;

import io.shrouded.okara.model.User;
import io.shrouded.okara.service.ReactiveFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class UserRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "users";

    public Mono<User> save(User user) {
        return firestoreService.save(COLLECTION_NAME, user, 
            user.getId(), (u, id) -> u.setId(id));
    }

    public Mono<User> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            User.class, (u, docId) -> u.setId(docId));
    }

    public Mono<User> findByUsername(String username) {
        return firestoreService.findFirstByField(COLLECTION_NAME, "username", username, 
            User.class, (u, docId) -> u.setId(docId));
    }

    public Mono<Boolean> existsByUsername(String username) {
        return firestoreService.existsByField(COLLECTION_NAME, "username", username);
    }

    public Mono<Boolean> existsByEmail(String email) {
        return firestoreService.existsByField(COLLECTION_NAME, "email", email);
    }

    public Flux<User> findAll() {
        return firestoreService.findAll(COLLECTION_NAME, User.class, (u, docId) -> u.setId(docId));
    }

    public Mono<Void> delete(User user) {
        return firestoreService.deleteById(COLLECTION_NAME, user.getId());
    }

}
