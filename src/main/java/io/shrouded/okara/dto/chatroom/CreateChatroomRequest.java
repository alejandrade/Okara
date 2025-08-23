package io.shrouded.okara.dto.chatroom;

import java.util.List;

public record CreateChatroomRequest(
        String name,
        String description,
        String imageUrl,
        String type, // PUBLIC, PRIVATE, DIRECT
        List<String> initialParticipants
) {
}