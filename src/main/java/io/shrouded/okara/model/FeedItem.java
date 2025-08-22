package io.shrouded.okara.model;

import com.google.cloud.Timestamp;
import io.shrouded.okara.enums.FeedType;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
public class FeedItem {
    private String postId;
    private String authorId;
    private String authorDisplayName;
    private String authorProfileImageUrl;

    private String content;
    private List<String> imageUrls;
    private String videoUrl;

    private FeedType postType;
    private Timestamp createdAt;
    private Timestamp addedToFeedAt;

    private Integer likesCount;
    private Integer commentsCount;

    private List<String> relevanceTags;
    private String reasonShown;
    private String chatroomId; // Which chatroom this feed item came from

    private String originalPostId;

    public FeedItem(Feed post) {
        this.postId = post.getId();
        this.authorId = post.getAuthorId();
        this.authorDisplayName = post.getAuthorDisplayName();
        this.authorProfileImageUrl = post.getAuthorProfileImageUrl();

        this.content = post.getContent();
        this.imageUrls = post.getImageUrls() != null ? post.getImageUrls() : new ArrayList<>();
        this.videoUrl = post.getVideoUrl();
        this.relevanceTags = new ArrayList<>();
        this.postType = post.getType();
        this.createdAt = post.getCreatedAt();
        this.addedToFeedAt = Timestamp.now();

        this.likesCount = post.getLikesCount();
        this.commentsCount = post.getCommentsCount();

        this.originalPostId = post.getOriginalPostId();
        this.chatroomId = null; // Will be set when creating specific chatroom feed items
    }

    public FeedItem(Feed post, String chatroomId) {
        this(post);
        this.chatroomId = chatroomId;
    }
}