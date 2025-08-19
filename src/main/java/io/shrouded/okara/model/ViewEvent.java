package io.shrouded.okara.model;

import com.google.cloud.firestore.annotation.DocumentId;
import com.google.cloud.spring.data.firestore.Document;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.google.cloud.Timestamp;

@Data
@NoArgsConstructor
@Document(collectionName = "view_events")
public class ViewEvent {
    
    @DocumentId
    private String id; // Format: {userId}_{postId}_{timestamp}
    
    private String userId;
    private String postId;
    private String postAuthorId;
    private Timestamp viewedAt;
    
    // View context
    private ViewSource viewSource; // PERSONAL_FEED, DISCOVERY_FEED, PROFILE, etc.
    private Long viewDurationMs; // How long they viewed it (optional)
    private String sessionId; // For tracking user sessions
    
    public enum ViewSource {
        PERSONAL_FEED,
        DISCOVERY_FEED, 
        PROFILE,
        SEARCH,
        DIRECT_LINK
    }
    
    public ViewEvent(String userId, String postId, String postAuthorId, ViewSource viewSource) {
        this.id = generateId(userId, postId);
        this.userId = userId;
        this.postId = postId;
        this.postAuthorId = postAuthorId;
        this.viewSource = viewSource;
        this.viewedAt = Timestamp.now();
    }
    
    public static String generateId(String userId, String postId) {
        return String.format("%s_%s_%d", userId, postId, System.currentTimeMillis());
    }
}