package io.shrouded.okara.dto.message;

import com.google.cloud.Timestamp;
import io.shrouded.okara.model.Message;

public record MessageDto(
        String id,
        String senderId,
        String receiverId,
        String conversationId,
        String encryptedContent,
        String signalPreKeyId,
        String signalSessionId,
        byte[] signalMessage,
        Timestamp sentAt,
        Timestamp deliveredAt,
        Timestamp readAt,
        boolean isDelivered,
        boolean isRead,
        boolean isDeleted
) {
    public static MessageDto fromMessage(Message message) {
        return new MessageDto(
                message.getId(),
                message.getSenderId(),
                message.getReceiverId(),
                message.getConversationId(),
                message.getEncryptedContent(),
                message.getSignalPreKeyId(),
                message.getSignalSessionId(),
                message.getSignalMessage(),
                message.getSentAt(),
                message.getDeliveredAt(),
                message.getReadAt(),
                message.isDelivered(),
                message.isRead(),
                message.isDeleted()
        );
    }
}