package io.shrouded.okara.controller;

import io.shrouded.okara.dto.media.MediaUploadResponse;
import io.shrouded.okara.service.CloudStorageService;
import io.shrouded.okara.service.CurrentUserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/media")
@RequiredArgsConstructor
@Slf4j
public class MediaController {

    private final CloudStorageService cloudStorageService;
    private final CurrentUserService currentUserService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<MediaUploadResponse>> uploadFile(@RequestPart("file") FilePart filePart) {
        log.info("Received file upload request: {}", filePart.filename());
        
        return currentUserService.getCurrentUser()
                .doOnNext(user -> log.info("User {} uploading file: {}", user.getId(), filePart.filename()))
                .flatMap(user -> cloudStorageService.uploadFile(filePart))
                .map(ResponseEntity::ok)
                .doOnSuccess(response -> log.info("File upload completed successfully"))
                .doOnError(error -> log.error("File upload failed: {}", error.getMessage()));
    }
}