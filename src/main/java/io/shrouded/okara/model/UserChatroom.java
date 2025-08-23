package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserChatroom {
    
    private String chatroomId; // Reference to global chatroom document ID
    private Timestamp joinedAt;
    private Timestamp lastReadAt;
    private Integer unreadCount = 0;
    private boolean isActive = true;
    private boolean isMuted = false;
    private boolean isPinned = false;
    
    public UserChatroom(String chatroomId, Timestamp joinedAt) {
        this.chatroomId = chatroomId;
        this.joinedAt = joinedAt;
        this.lastReadAt = joinedAt;
    }
}