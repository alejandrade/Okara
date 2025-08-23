package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class Chatroom {

    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private ChatroomType type;
    private Integer participantCount = 0;
    private String createdBy; // Firebase UID of creator
    private Timestamp createdAt;
    private Timestamp lastActivity;
    private boolean isActive = true;
    private List<String> participants = new ArrayList<>(); // Firebase UIDs

    public enum ChatroomType {
        PUBLIC, PRIVATE, DIRECT
    }

    public String getNameLowerCare() {
        return name.toLowerCase();
    }
}