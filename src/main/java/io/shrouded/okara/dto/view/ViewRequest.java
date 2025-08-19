package io.shrouded.okara.dto.view;

import io.shrouded.okara.enums.ViewSource;

public record ViewRequest(
        String postId,
        String postAuthorId,
        ViewSource viewSource,
        Long viewDurationMs
) {
    public ViewRequest {
        if (viewSource == null) {
            viewSource = ViewSource.PERSONAL_FEED;
        }
    }
}