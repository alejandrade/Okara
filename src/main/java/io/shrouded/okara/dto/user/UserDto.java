package io.shrouded.okara.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
@Schema(description = "User profile information")
public record UserDto(
        @Schema(description = "Unique user identifier", example = "user123")
        String id,
        @Schema(description = "Username", example = "john_doe")
        String username,
        @Schema(description = "Email address", example = "john@example.com")
        String email,
        @Schema(description = "Display name", example = "John Doe")
        String displayName,
        @Schema(description = "User bio", example = "Software developer")
        String bio,
        @Schema(description = "Profile image URL")
        String profileImageUrl,
        @Schema(description = "Banner image URL")
        String bannerImageUrl,
        @Schema(description = "User location", example = "San Francisco, CA")
        String location,
        @Schema(description = "Personal website URL", example = "https://johndoe.com")
        String website,
        @Schema(description = "Account creation timestamp")
        Instant createdAt,
        @Schema(description = "Last update timestamp")
        Instant updatedAt,
        @Schema(description = "List of user IDs that this user follows")
        List<String> following,
        @Schema(description = "List of user IDs that follow this user")
        List<String> followers,
        @Schema(description = "Number of users being followed", example = "150")
        Integer followingCount,
        @Schema(description = "Number of followers", example = "200")
        Integer followersCount,
        @Schema(description = "Total number of posts", example = "42")
        Integer postsCount,
        @Schema(description = "Whether the user is verified", example = "false")
        Boolean verified,
        @Schema(description = "Whether the profile is private", example = "false")
        Boolean isPrivate
) {
}