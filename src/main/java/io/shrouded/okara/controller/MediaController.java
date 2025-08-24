package io.shrouded.okara.controller;

import io.shrouded.okara.dto.media.MediaUploadResponse;
import io.shrouded.okara.service.CloudStorageService;
import io.shrouded.okara.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Media", description = "Media file upload and management endpoints")
@SecurityRequirement(name = "bearerAuth")
public class MediaController {

    private final CloudStorageService cloudStorageService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload media file", description = "Uploads a media file (image, video, etc.) and returns the file URL")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "File uploaded successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = MediaUploadResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid file or file too large",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<MediaUploadResponse> uploadFile(
            @Parameter(description = "Media file to upload", required = true)
            @RequestPart("file") FilePart filePart) {
        log.info("Received file upload request: {}", filePart.filename());
        
        return currentUserService.getCurrentUser()
                .doOnNext(user -> log.info("User {} uploading file: {}", user.getId(), filePart.filename()))
                .flatMap(user -> cloudStorageService.uploadFile(filePart))
                .doOnSuccess(response -> log.info("File upload completed successfully"))
                .doOnError(error -> log.error("File upload failed: {}", error.getMessage()));
    }
}