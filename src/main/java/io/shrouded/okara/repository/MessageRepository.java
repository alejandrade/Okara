package io.shrouded.okara.repository;

import com.google.cloud.firestore.Query;
import io.shrouded.okara.model.Message;
import io.shrouded.okara.service.ReactiveFirestoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
@Slf4j
public class MessageRepository {

    private final ReactiveFirestoreService firestoreService;
    private static final String COLLECTION_NAME = "messages";

    public Mono<Message> save(Message message) {
        return firestoreService.save(COLLECTION_NAME, message, 
            message::getId, (m, id) -> m.setId(id));
    }

    public Mono<Message> findById(String id) {
        return firestoreService.findById(COLLECTION_NAME, id, 
            Message.class, (m, docId) -> m.setId(docId));
    }

    // Get messages in a conversation ordered by sent time (newest first)
    public Flux<Message> findByConversationIdOrderBySentAtDesc(String conversationId, int limit) {
        return firestoreService.findByFieldOrderByWithLimit(COLLECTION_NAME, "conversationId", conversationId, 
            "sentAt", Query.Direction.DESCENDING, limit, Message.class, (m, docId) -> m.setId(docId));
    }
    
    // Get messages in a conversation with pagination
    public Flux<Message> findByConversationIdAndIdLessThanOrderBySentAtDesc(String conversationId, String lastMessageId, int limit) {
        // Note: This is a simplification. For proper pagination by ID, you'd need to
        // first get the lastMessage's timestamp and then query by timestamp
        // since Firestore doesn't support direct ID comparison in this way
        return firestoreService.findByFieldOrderByWithLimit(COLLECTION_NAME, "conversationId", conversationId, 
            "sentAt", Query.Direction.DESCENDING, limit, Message.class, (m, docId) -> m.setId(docId));
    }

    // Get messages by sender
    public Flux<Message> findBySenderIdOrderBySentAtDesc(String senderId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "senderId", senderId, 
            "sentAt", Query.Direction.DESCENDING, Message.class, (m, docId) -> m.setId(docId));
    }
    
    // Get messages by receiver
    public Flux<Message> findByReceiverIdOrderBySentAtDesc(String receiverId) {
        return firestoreService.findByFieldOrderBy(COLLECTION_NAME, "receiverId", receiverId, 
            "sentAt", Query.Direction.DESCENDING, Message.class, (m, docId) -> m.setId(docId));
    }

    public Mono<Void> delete(Message message) {
        return firestoreService.delete(COLLECTION_NAME, message, 
            message::getId);
    }

    public Mono<Void> deleteById(String id) {
        return firestoreService.deleteById(COLLECTION_NAME, id);
    }
}