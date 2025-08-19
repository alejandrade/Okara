package io.shrouded.okara.dto.media;

public record MediaUploadResponse(
    String fileName,
    String url,
    String contentType,
    Long size
) {}