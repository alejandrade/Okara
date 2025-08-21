package io.shrouded.okara.repository;

import com.google.cloud.spring.data.firestore.FirestoreReactiveRepository;
import io.shrouded.okara.model.Message;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface MessageRepository extends FirestoreReactiveRepository<Message> {

    // Get messages in a conversation ordered by sent time (newest first)
    Flux<Message> findByConversationIdOrderBySentAtDesc(String conversationId, int limit);
    
    // Get messages in a conversation with pagination
    Flux<Message> findByConversationIdAndIdLessThanOrderBySentAtDesc(String conversationId, String lastMessageId, int limit);

    // Get messages by sender
    Flux<Message> findBySenderIdOrderBySentAtDesc(String senderId);
    
    // Get messages by receiver
    Flux<Message> findByReceiverIdOrderBySentAtDesc(String receiverId);
}