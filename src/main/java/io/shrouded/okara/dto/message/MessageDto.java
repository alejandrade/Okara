package io.shrouded.okara.dto.message;

import com.google.cloud.Timestamp;
import io.shrouded.okara.model.Message;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "End-to-end encrypted message information")
public record MessageDto(
        @Schema(description = "Unique message identifier", example = "msg123")
        String id,
        @Schema(description = "Sender's user ID", example = "user456")
        String senderId,
        @Schema(description = "Receiver's user ID", example = "user789")
        String receiverId,
        @Schema(description = "Conversation ID", example = "conv123")
        String conversationId,
        @Schema(description = "Encrypted message content")
        String encryptedContent,
        @Schema(description = "Signal Protocol pre-key ID for encryption")
        String signalPreKeyId,
        @Schema(description = "Signal Protocol session ID")
        String signalSessionId,
        @Schema(description = "Signal Protocol encrypted message bytes")
        byte[] signalMessage,
        @Schema(description = "Message sent timestamp")
        Timestamp sentAt,
        @Schema(description = "Message delivered timestamp")
        Timestamp deliveredAt,
        @Schema(description = "Message read timestamp")
        Timestamp readAt,
        @Schema(description = "Whether the message has been delivered", example = "true")
        boolean isDelivered,
        @Schema(description = "Whether the message has been read", example = "false")
        boolean isRead,
        @Schema(description = "Whether the message has been deleted", example = "false")
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