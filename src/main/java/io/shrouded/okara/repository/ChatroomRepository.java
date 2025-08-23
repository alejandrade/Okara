package io.shrouded.okara.repository;

import com.google.cloud.firestore.Query;
import io.shrouded.okara.model.Chatroom;
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
public class ChatroomRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "chatrooms";

    public Mono<Chatroom> save(Chatroom chatroom) {
        return firestoreService.save(COLLECTION_NAME, chatroom, 
            chatroom.getId(), (c, id) -> c.setId(id));
    }

    public Mono<Chatroom> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            Chatroom.class, (c, docId) -> c.setId(docId));
    }

    public Flux<Chatroom> findByNameIgnoreCase(String name) {
        // Query by lowercase name field for case-insensitive exact match
        // This method finds chatrooms with exact name matches (case-insensitive)
        // For more advanced search (partial matches, starts with, etc.), 
        // consider implementing a text search service like Algolia or Elasticsearch
        String lowercaseName = name.toLowerCase();
        return firestoreService.findByField(COLLECTION_NAME, "nameLowercase", lowercaseName, 
            Chatroom.class, (c, docId) -> c.setId(docId));
    }

    public Flux<Chatroom> searchChatroomsByName(String searchTerm) {
        // Advanced search method for partial matches
        // This is a placeholder for future implementation
        // For now, returns exact matches, but could be extended with:
        // - Text search service integration (Algolia, Elasticsearch)
        // - Prefix matching with Firestore queries
        // - Fuzzy search algorithms
        return findByNameIgnoreCase(searchTerm);
    }

    public Flux<Chatroom> findByType(Chatroom.ChatroomType type) {
        return firestoreService.findByField(COLLECTION_NAME, "type", type.name(), 
            Chatroom.class, (c, docId) -> c.setId(docId));
    }

    public Flux<Chatroom> findByIsActive(boolean isActive) {
        return firestoreService.findByField(COLLECTION_NAME, "active", isActive, 
            Chatroom.class, (c, docId) -> c.setId(docId));
    }

    public Flux<Chatroom> findByParticipantsContaining(String firebaseUid) {
        return firestoreService.findByArrayContains(COLLECTION_NAME, "participants", firebaseUid, 
            Chatroom.class, (c, docId) -> c.setId(docId));
    }

    public Mono<Boolean> existsByName(String name) {
        // Check if a chatroom with the exact name exists (case-insensitive)
        String lowercaseName = name.toLowerCase();
        return firestoreService.existsByField(COLLECTION_NAME, "nameLowercase", lowercaseName);
    }

    public Flux<Chatroom> findByIdsIn(List<String> ids) {
        return firestoreService.findByIdsIn(COLLECTION_NAME, ids,
                                            Chatroom.class, Chatroom::setId);
    }

    public Mono<Void> delete(Chatroom chatroom) {
        return firestoreService.deleteById(COLLECTION_NAME, chatroom.getId());
    }

}