package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class Message {
    
    private String id;
    private String senderId;
    private String receiverId;
    private String conversationId;
    
    // Signal Protocol specific fields
    private String encryptedContent;
    private String signalPreKeyId;
    private String signalSessionId;
    private byte[] signalMessage;
    
    // Metadata
    private Timestamp sentAt;
    private Timestamp deliveredAt;
    private Timestamp readAt;
    private boolean isDelivered;
    private boolean isRead;
    private boolean isDeleted;
    
    // Message type
    private MessageType messageType;
    
    public Message(String senderId, String receiverId, String encryptedContent, 
                   String signalPreKeyId, String signalSessionId, byte[] signalMessage) {
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.encryptedContent = encryptedContent;
        this.signalPreKeyId = signalPreKeyId;
        this.signalSessionId = signalSessionId;
        this.signalMessage = signalMessage;
        this.conversationId = generateConversationId(senderId, receiverId);
        this.sentAt = Timestamp.now();
        this.messageType = MessageType.TEXT;
        this.isDelivered = false;
        this.isRead = false;
        this.isDeleted = false;
    }
    
    private String generateConversationId(String userId1, String userId2) {
        // Ensure consistent conversation ID regardless of sender/receiver order
        if (userId1.compareTo(userId2) < 0) {
            return String.format("%s_%s", userId1, userId2);
        } else {
            return String.format("%s_%s", userId2, userId1);
        }
    }
    
    public void markAsDelivered() {
        this.isDelivered = true;
        this.deliveredAt = Timestamp.now();
    }
    
    public void markAsRead() {
        this.isRead = true;
        this.readAt = Timestamp.now();
    }
    
    public void markAsDeleted() {
        this.isDeleted = true;
    }
    
    public enum MessageType {
        TEXT, IMAGE, VIDEO, AUDIO, FILE, SYSTEM
    }
}