package io.shrouded.okara.dto.chatroom;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.shrouded.okara.model.Chatroom;
import io.shrouded.okara.model.UserChatroom;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(description = "Chatroom information")
public record ChatroomDto(
        @Schema(description = "Unique chatroom identifier", example = "chatroom123")
        String id,
        @Schema(description = "Chatroom name", example = "General Discussion")
        String name,
        @Schema(description = "Chatroom description", example = "A place for general discussions")
        String description,
        @Schema(description = "Chatroom image URL")
        String imageUrl,
        @Schema(description = "Chatroom type", example = "PUBLIC")
        String type,
        @Schema(description = "Number of participants", example = "25")
        Integer participantCount,
        @Schema(description = "Last activity timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant lastActivity,
        @Schema(description = "Number of unread messages for the user", example = "3")
        Integer unreadCount,
        @Schema(description = "Whether the chatroom is active", example = "true")
        Boolean isActive,
        @Schema(description = "User ID of the creator", example = "user456")
        String createdBy,
        @Schema(description = "Creation timestamp")
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant createdAt,
        @Schema(description = "List of participant user IDs")
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