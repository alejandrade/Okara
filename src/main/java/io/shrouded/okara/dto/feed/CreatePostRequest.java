package io.shrouded.okara.dto.feed;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreatePostRequest(
        @NotBlank(message = "Content is required and cannot be blank")
        @Size(max = 2000, message = "Content cannot exceed 2000 characters")
        String content,
        
        List<String> imageUrls,
        String videoUrl,
        
        @NotEmpty(message = "At least one chatroom ID is required")
        @Size(max = 3, message = "Cannot post to more than 3 chatrooms at once")
        List<String> chatroomIds
) {
}