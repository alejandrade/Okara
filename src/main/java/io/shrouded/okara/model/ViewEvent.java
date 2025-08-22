package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import io.shrouded.okara.enums.ViewSource;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ViewEvent {

    private String id; // Format: {userId}_{postId}_{timestamp}

    private String userId;
    private String postId;
    private String postAuthorId;
    private Timestamp viewedAt;

    // View context
    private ViewSource viewSource; // PERSONAL_FEED, DISCOVERY_FEED, PROFILE, etc.
    private Long viewDurationMs; // How long they viewed it (optional)
    private String sessionId; // For tracking user sessions


    public ViewEvent(String userId, String postId, String postAuthorId, ViewSource viewSource) {
        this.userId = userId;
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.viewSource = viewSource;
        this.viewedAt = Timestamp.now();
    }
}