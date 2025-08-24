package io.shrouded.okara.dto.feed;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.shrouded.okara.enums.FeedType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Schema(description = "Social feed post information")
public record FeedDto(
        @Schema(description = "Unique post identifier", example = "post123")
        String id,
        @Schema(description = "Post author's user ID", example = "user456")
        String authorId,
        @Schema(description = "Author's display name", example = "John Doe")
        String authorDisplayName,
        @Schema(description = "Author's profile image URL")
        String authorProfileImageUrl,
        @Schema(description = "Post content/text", example = "Hello world! This is my first post.")
        String content,
        @Schema(description = "List of image URLs attached to the post")
        List<String> imageUrls,
        @Schema(description = "Video URL if attached")
        String videoUrl,
        @Schema(description = "Type of feed post (POST, COMMENT, RETWEET, etc.)")
        FeedType type,
        @Schema(description = "Parent post ID for comments or replies")
        String parentId,
        @Schema(description = "Root post ID for threaded discussions")
        String rootId,
        @Schema(description = "Post creation timestamp")
        Instant createdAt,
        @Schema(description = "Last update timestamp")
        Instant updatedAt,
        @Schema(description = "List of user IDs who liked this post")
        List<String> likedBy,
        @Schema(description = "List of user IDs who disliked this post")
        List<String> dislikedBy,
        @Schema(description = "Total number of likes", example = "42")
        Integer likesCount,
        @Schema(description = "Total number of dislikes", example = "2")
        Integer dislikesCount,
        @Schema(description = "Total number of comments", example = "15")
        Integer commentsCount,
        @Schema(description = "Total number of views", example = "1250")
        Integer viewsCount,
        @Schema(description = "Number of unique users who commented", example = "8")
        Integer distinctCommentersCount,
        @Schema(description = "Post categories/niches")
        List<String> niches,
        @Schema(description = "Original post ID for retweets or cross-posts")
        String originalPostId,
        @Schema(description = "Hashtags extracted from content")
        List<String> hashtags,
        @Schema(description = "User mentions in the post")
        List<String> mentions,
        @Schema(description = "Chatroom IDs where this post was shared")
        List<String> chatroomIds
) {
}