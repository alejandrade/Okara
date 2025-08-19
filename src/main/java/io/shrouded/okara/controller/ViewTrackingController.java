package io.shrouded.okara.controller;

import io.shrouded.okara.dto.view.ViewCountResponse;
import io.shrouded.okara.dto.view.ViewRequest;
import io.shrouded.okara.dto.view.ViewResponse;
import io.shrouded.okara.dto.view.ViewedResponse;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.ViewTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;


@RestController
@RequestMapping("/api/views")
@RequiredArgsConstructor
@Slf4j
public class ViewTrackingController {

    private final ViewTrackingService viewTrackingService;
    private final CurrentUserService currentUserService;

    /**
     * Record that a user viewed a feed item
     */
    @PostMapping("/record")
    public Mono<ResponseEntity<ViewResponse>> recordView(@RequestBody final ViewRequest request) {
        return currentUserService.getCurrentUserId()
                                 .flatMap(userId -> viewTrackingService.recordFeedItemView(
                                         userId,
                                         request.postId(),
                                         request.postAuthorId(),
                                         request.viewSource(),
                                         request.viewDurationMs()
                                 ))
                                 .then(Mono.just(ResponseEntity.ok(new ViewResponse("View recorded successfully"))))
                                 .onErrorResume(e -> {
                                     log.error("Failed to record view: {}", e.getMessage());
                                     return Mono.just(ResponseEntity.badRequest()
                                                                    .body(new ViewResponse("Failed to record view: " + e.getMessage())));
                                 });
    }

    /**
     * Get view count for a specific post
     */
    @GetMapping("/post/{postId}/count")
    public Mono<ResponseEntity<ViewCountResponse>> getPostViewCount(@PathVariable String postId) {
        return viewTrackingService.getPostViewCount(postId)
                                  .map(count -> ResponseEntity.ok(new ViewCountResponse(count)))
                                  .onErrorResume(e -> {
                                      log.error("Failed to get post view count: {}", e.getMessage());
                                      return Mono.just(ResponseEntity.badRequest()
                                                                     .body(new ViewCountResponse(0L)));
                                  });
    }

    /**
     * Get current user's total view count
     */
    @GetMapping("/user/count")
    public Mono<ResponseEntity<ViewCountResponse>> getCurrentUserViewCount() {
        return currentUserService.getCurrentUserId()
                                 .flatMap(viewTrackingService::getUserViewCount)
                                 .map(count -> ResponseEntity.ok(new ViewCountResponse(count)))
                                 .onErrorResume(e -> {
                                     log.error("Failed to get user view count: {}", e.getMessage());
                                     return Mono.just(ResponseEntity.badRequest()
                                                                    .body(new ViewCountResponse(0L)));
                                 });
    }

    /**
     * Check if current user has viewed a specific post
     */
    @GetMapping("/post/{postId}/viewed")
    public Mono<ResponseEntity<ViewedResponse>> hasViewedPost(@PathVariable String postId) {
        return currentUserService.getCurrentUserId()
                                 .flatMap(userId -> viewTrackingService.hasUserViewedPost(userId, postId))
                                 .map(viewed -> ResponseEntity.ok(new ViewedResponse(viewed)))
                                 .onErrorResume(e -> {
                                     log.error("Failed to check if post was viewed: {}", e.getMessage());
                                     return Mono.just(ResponseEntity.badRequest()
                                                                    .body(new ViewedResponse(false)));
                                 });
    }
}