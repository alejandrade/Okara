package io.shrouded.okara.dto.chatroom;

import java.util.List;

public record ChatroomListResponse(
        List<ChatroomDto> chatrooms,
        Boolean hasMore,
        String nextCursor,
        Integer total
) {
    
    public static ChatroomListResponse of(List<ChatroomDto> chatrooms, boolean hasMore, String nextCursor, int total) {
        return new ChatroomListResponse(chatrooms, hasMore, nextCursor, total);
    }
}