package io.shrouded.okara.dto.user;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;

import java.time.Instant;
import java.util.List;

@JsonInclude(JsonInclude.Include.NON_NULL)
@Builder
public record UserDto(
        String id,
        String username,
        String email,
        String displayName,
        String bio,
        String profileImageUrl,
        String bannerImageUrl,
        String location,
        String website,
        Instant createdAt,
        Instant updatedAt,
        List<String> following,
        List<String> followers,
        Integer followingCount,
        Integer followersCount,
        Integer postsCount,
        Boolean verified,
        Boolean isPrivate
) {
}