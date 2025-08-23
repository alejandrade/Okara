package io.shrouded.okara.dto.feed;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CrossPostRequest(
        @NotEmpty(message = "At least one chatroom ID is required")
        @Size(max = 3, message = "Cannot cross-post to more than 3 chatrooms at once")
        List<String> chatroomIds
) {
}