package io.shrouded.okara.dto.chatroom;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.shrouded.okara.model.Chatroom;
import io.shrouded.okara.model.UserChatroom;

import java.time.Instant;
import java.util.List;

public record ChatroomDto(
        String id,
        String name,
        String description,
        String imageUrl,
        String type,
        Integer participantCount,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant lastActivity,
        Integer unreadCount,
        Boolean isActive,
        String createdBy,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,
        List<String> participants
) {

    public static ChatroomDto fromChatroom(Chatroom chatroom, UserChatroom userChatroom) {
        return new ChatroomDto(
                chatroom.getId(),
                chatroom.getName(),
                chatroom.getDescription(),
                chatroom.getImageUrl(),
                chatroom.getType() != null ? chatroom.getType().name() : null,
                chatroom.getParticipantCount(),
                chatroom.getLastActivity() != null ? chatroom.getLastActivity().toDate().toInstant() : null,
                userChatroom != null ? userChatroom.getUnreadCount() : 0,
                chatroom.isActive(),
                chatroom.getCreatedBy(),
                chatroom.getCreatedAt() != null ? chatroom.getCreatedAt().toDate().toInstant() : null,
                chatroom.getParticipants()
        );
    }
}