package io.shrouded.okara.dto.event;

import io.shrouded.okara.enums.FeedEventType;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class FeedEvent {
    private FeedEventType eventType;
    private String postId;
    private String authorId;
    private String authorUsername;
    private String content;
    private String createdAt;
    private List<String> chatroomIds; // Chatrooms this post should be distributed to
    private FeedEventMetadata metadata;
}