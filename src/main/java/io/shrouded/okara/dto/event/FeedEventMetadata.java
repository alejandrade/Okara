package io.shrouded.okara.dto.event;

import io.shrouded.okara.enums.FeedType;
import java.util.List;

public record FeedEventMetadata(
    String postType,
    Integer likesCount,
    Integer retweetsCount,
    Integer commentsCount,
    Double baseEngagementScore,
    List<String> hashtags,
    List<String> mentions,
    Boolean hasImages,
    Integer imageCount,
    Boolean hasVideo,
    Boolean isReply,
    String parentId,
    String followerId,
    String followedId,
    String unfollowedId
) {
    public static FeedEventMetadata forPost(FeedType postType, Integer likesCount, Integer retweetsCount, 
                                           Integer commentsCount, Double baseEngagementScore, 
                                           List<String> hashtags, List<String> mentions, 
                                           Boolean hasImages, Integer imageCount, Boolean hasVideo,
                                           Boolean isReply, String parentId) {
        return new FeedEventMetadata(
            postType != null ? postType.name() : null,
            likesCount, retweetsCount, commentsCount, baseEngagementScore,
            hashtags, mentions, hasImages, imageCount, hasVideo, isReply, parentId,
            null, null, null
        );
    }
    
    public static FeedEventMetadata forFollow(String followerId, String followedId) {
        return new FeedEventMetadata(
            null, null, null, null, null, null, null, null, null, null, null, null,
            followerId, followedId, null
        );
    }
    
    public static FeedEventMetadata forUnfollow(String followerId, String unfollowedId) {
        return new FeedEventMetadata(
            null, null, null, null, null, null, null, null, null, null, null, null,
            followerId, null, unfollowedId
        );
    }
}