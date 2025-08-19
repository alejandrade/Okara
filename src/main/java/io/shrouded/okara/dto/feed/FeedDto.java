package io.shrouded.okara.dto.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.shrouded.okara.enums.FeedType;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record FeedDto(
        String id,
        String authorId,
        String authorUsername,
        String authorDisplayName,
        String authorProfileImageUrl,
        String content,
        List<String> imageUrls,
        String videoUrl,
        FeedType type,
        String parentId,
        String rootId,
        Instant createdAt,
        Instant updatedAt,
        List<String> likedBy,
        List<String> dislikedBy,
        List<String> retweetedBy,
        Integer likesCount,
        Integer dislikesCount,
        Integer retweetsCount,
        Integer commentsCount,
        Integer viewsCount,
        Double baseEngagementScore,
        Integer distinctCommentersCount,
        List<String> niches,
        String originalPostId,
        String quoteTweetComment,
        Boolean isPinned,
        List<String> hashtags,
        List<String> mentions
) {
}