package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import io.shrouded.okara.model.Message;
import io.shrouded.okara.repository.MessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class MessageService {

    private final MessageRepository messageRepository;

    public Mono<Message> sendMessage(String senderId, String receiverId, String encryptedContent,
                                   String signalPreKeyId, String signalSessionId, byte[] signalMessage) {
        Message message = new Message(senderId, receiverId, encryptedContent, 
                                     signalPreKeyId, signalSessionId, signalMessage);
        
        return messageRepository.save(message)
                .doOnSuccess(savedMessage -> 
                    log.info("Message sent from {} to {}", senderId, receiverId));
    }


    public Flux<Message> getConversation(String userId1, String userId2, int limit, String lastMessageId) {
        String conversationId = generateConversationId(userId1, userId2);
        
        if (lastMessageId != null) {
            return messageRepository.findByConversationIdAndIdLessThanOrderBySentAtDesc(
                    conversationId, lastMessageId, limit);
        } else {
            return messageRepository.findByConversationIdOrderBySentAtDesc(conversationId, limit);
        }
    }


    public Mono<Message> markAsDelivered(String messageId, String userId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getReceiverId().equals(userId))
                .flatMap(message -> {
                    message.markAsDelivered();
                    return messageRepository.save(message);
                });
    }

    public Mono<Message> markAsRead(String messageId, String userId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getReceiverId().equals(userId))
                .flatMap(message -> {
                    message.markAsRead();
                    return messageRepository.save(message);
                });
    }

    public Mono<Message> deleteMessage(String messageId, String userId) {
        return messageRepository.findById(messageId)
                .filter(message -> message.getSenderId().equals(userId))
                .flatMap(message -> {
                    message.markAsDeleted();
                    return messageRepository.save(message);
                });
    }

    public Flux<String> getRecentConversations(String userId, int limit) {
        // Get messages where user is sender
        Flux<String> fromSentMessages = messageRepository.findBySenderIdOrderBySentAtDesc(userId)
                .map(Message::getReceiverId);
                
        // Get messages where user is receiver
        Flux<String> fromReceivedMessages = messageRepository.findByReceiverIdOrderBySentAtDesc(userId)
                .map(Message::getSenderId);
                
        // Merge and deduplicate
        return Flux.merge(fromSentMessages, fromReceivedMessages)
                .distinct()
                .take(limit);
    }

    public Mono<Long> getUnreadCount(String userId) {
        return messageRepository.findByReceiverIdOrderBySentAtDesc(userId)
                .filter(message -> !message.isRead() && !message.isDeleted())
                .count();
    }

    private String generateConversationId(String userId1, String userId2) {
        if (userId1.compareTo(userId2) < 0) {
            return String.format("%s_%s", userId1, userId2);
        } else {
            return String.format("%s_%s", userId2, userId1);
        }
    }
}