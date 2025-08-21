package io.shrouded.okara.dto.message;

public record SendMessageRequest(
        String receiverId,
        String encryptedContent,
        String signalPreKeyId,
        String signalSessionId,
        byte[] signalMessage
) {
}