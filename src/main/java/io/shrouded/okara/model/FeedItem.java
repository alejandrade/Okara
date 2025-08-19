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
    private String authorUsername;
    private String authorDisplayName;
    private String authorProfileImageUrl;

    private String content;
    private List<String> imageUrls;
    private String videoUrl;

    private FeedType postType;
    private Timestamp createdAt;
    private Timestamp addedToFeedAt;

    private Integer likesCount;
    private Integer retweetsCount ;
    private Integer commentsCount;

    private Double algorithmScore;
    private List<String> relevanceTags;
    private String reasonShown;

    private String originalPostId;
    private String retweetComment;

    public FeedItem(Feed post) {
        this.postId = post.getId();
        this.authorId = post.getAuthorId();
        this.authorUsername = post.getAuthorUsername();
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
        this.retweetsCount = post.getRetweetsCount();
        this.commentsCount = post.getCommentsCount();

        this.algorithmScore = post.getBaseEngagementScore();

        this.originalPostId = post.getOriginalPostId();
        this.retweetComment = post.getQuoteTweetComment();
    }
}