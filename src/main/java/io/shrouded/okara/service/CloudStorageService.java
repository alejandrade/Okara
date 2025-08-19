package io.shrouded.okara.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import io.shrouded.okara.dto.media.MediaUploadResponse;
import io.shrouded.okara.exception.OkaraException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudStorageService {

    private final Storage storage;

    @Value("${gcp.storage.bucket-name}")
    private String bucketName;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
        "image/jpeg", "image/jpg", "image/png", "image/gif", "image/webp"
    );

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of(
        "video/mp4", "video/mpeg", "video/quicktime", "video/x-msvideo", "video/webm"
    );

    private static final long MAX_FILE_SIZE = 100 * 1024 * 1024; // 100MB

    public Mono<MediaUploadResponse> uploadFile(FilePart filePart) {
        return validateFile(filePart)
                .flatMap(this::processUpload);
    }

    private Mono<FilePart> validateFile(FilePart filePart) {
        String contentType = filePart.headers().getContentType() != null 
            ? filePart.headers().getContentType().toString() 
            : "";

        // Validate content type
        if (!ALLOWED_IMAGE_TYPES.contains(contentType) && !ALLOWED_VIDEO_TYPES.contains(contentType)) {
            return Mono.error(OkaraException.badRequest("File must be an image or video. Allowed types: " 
                + ALLOWED_IMAGE_TYPES + ", " + ALLOWED_VIDEO_TYPES));
        }

        // Validate filename extension
        String filename = filePart.filename();
        if (filename == null || filename.isEmpty()) {
            return Mono.error(OkaraException.badRequest("Filename is required"));
        }

        String extension = getFileExtension(filename).toLowerCase();
        if (!isValidExtension(extension, contentType)) {
            return Mono.error(OkaraException.badRequest("File extension does not match content type"));
        }

        return Mono.just(filePart);
    }

    private Mono<MediaUploadResponse> processUpload(FilePart filePart) {
        return DataBufferUtils.join(filePart.content())
                .flatMap(dataBuffer -> {
                    try {
                        byte[] bytes = extractBytes(dataBuffer);
                        
                        // Validate file size
                        if (bytes.length > MAX_FILE_SIZE) {
                            return Mono.error(OkaraException.badRequest("File size exceeds maximum limit of 100MB"));
                        }

                        // Generate unique filename
                        String originalFilename = filePart.filename();
                        String extension = getFileExtension(originalFilename);
                        String uniqueFilename = UUID.randomUUID() + "." + extension;
                        String contentType = Objects.requireNonNull(filePart.headers().getContentType()).toString();

                        // Upload to Google Cloud Storage
                        BlobId blobId = BlobId.of(bucketName, uniqueFilename);
                        BlobInfo blobInfo = BlobInfo.newBuilder(blobId)
                                .setContentType(contentType)
                                .build();

                        storage.create(blobInfo, bytes);
                        
                        String publicUrl = String.format("https://storage.googleapis.com/%s/%s", 
                            bucketName, uniqueFilename);

                        log.info("Successfully uploaded file: {} to bucket: {}", uniqueFilename, bucketName);

                        return Mono.just(new MediaUploadResponse(
                            uniqueFilename,
                            publicUrl,
                            contentType,
                            (long) bytes.length
                        ));

                    } catch (Exception e) {
                        log.error("Failed to upload file: {}", e.getMessage(), e);
                        return Mono.error(OkaraException.internalError("Failed to upload file"));
                    } finally {
                        DataBufferUtils.release(dataBuffer);
                    }
                });
    }

    private byte[] extractBytes(DataBuffer dataBuffer) throws IOException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        outputStream.write(dataBuffer.asByteBuffer().array());
        return outputStream.toByteArray();
    }

    private String getFileExtension(String filename) {
        int lastDotIndex = filename.lastIndexOf('.');
        return lastDotIndex > 0 ? filename.substring(lastDotIndex + 1) : "";
    }

    private boolean isValidExtension(String extension, String contentType) {
        return switch (extension) {
            case "jpg", "jpeg" -> contentType.equals("image/jpeg") || contentType.equals("image/jpg");
            case "png" -> contentType.equals("image/png");
            case "gif" -> contentType.equals("image/gif");
            case "webp" -> contentType.equals("image/webp");
            case "mp4" -> contentType.equals("video/mp4");
            case "mpeg", "mpg" -> contentType.equals("video/mpeg");
            case "mov" -> contentType.equals("video/quicktime");
            case "avi" -> contentType.equals("video/x-msvideo");
            case "webm" -> contentType.equals("video/webm");
            default -> false;
        };
    }
}