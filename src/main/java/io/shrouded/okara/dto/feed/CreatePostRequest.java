package io.shrouded.okara.dto.feed;

import java.util.List;

public record CreatePostRequest(
        String content,
        List<String> imageUrls,
        String videoUrl
) {
}