package io.shrouded.okara.controller;

import io.shrouded.okara.dto.view.ViewCountResponse;
import io.shrouded.okara.dto.view.ViewRequest;
import io.shrouded.okara.dto.view.ViewResponse;
import io.shrouded.okara.dto.view.ViewedResponse;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.ViewTrackingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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
@RequestMapping("/api/views")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "View Tracking", description = "User view tracking and analytics endpoints")
@SecurityRequirement(name = "bearerAuth")
public class ViewTrackingController {

    private final ViewTrackingService viewTrackingService;
    private final CurrentUserService currentUserService;

    /**
     * Record that a user viewed a feed item
     */
    @PostMapping("/record")
    @Operation(summary = "Record view", description = "Records that a user viewed a feed item for analytics purposes")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "View recorded successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ViewResponse.class))),
        @ApiResponse(responseCode = "400", description = "Invalid view data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ViewResponse> recordView(
            @Parameter(description = "View tracking request", required = true)
            @RequestBody final ViewRequest request) {
        return currentUserService.getCurrentUserId()
                                 .flatMap(userId -> viewTrackingService.recordFeedItemView(
                                         userId,
                                         request.postId(),
                                         request.postAuthorId(),
                                         request.viewSource(),
                                         request.viewDurationMs()
                                 ))
                                 .then(Mono.just(new ViewResponse("View recorded successfully")))
                                 .onErrorMap(e -> {
                                     log.error("Failed to record view: {}", e.getMessage());
                                     return OkaraException.badRequest("Failed to record view: " + e.getMessage());
                                 });
    }

    /**
     * Get view count for a specific post
     */
    @GetMapping("/post/{postId}/count")
    @Operation(summary = "Get post view count", description = "Retrieves the total number of views for a specific post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "View count retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ViewCountResponse.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ViewCountResponse> getPostViewCount(
            @Parameter(description = "ID of the post", required = true)
            @PathVariable String postId) {
        return viewTrackingService.getPostViewCount(postId)
                                  .map(ViewCountResponse::new)
                                  .onErrorReturn(new ViewCountResponse(0L))
                                  .doOnError(e -> log.error("Failed to get post view count: {}", e.getMessage()));
    }

    /**
     * Get current user's total view count
     */
    @GetMapping("/user/count")
    @Operation(summary = "Get user view count", description = "Retrieves the total number of views received by the authenticated user's posts")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User view count retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ViewCountResponse.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ViewCountResponse> getCurrentUserViewCount() {
        return currentUserService.getCurrentUserId()
                                 .flatMap(viewTrackingService::getUserViewCount)
                                 .map(ViewCountResponse::new)
                                 .onErrorReturn(new ViewCountResponse(0L))
                                 .doOnError(e -> log.error("Failed to get user view count: {}", e.getMessage()));
    }

    /**
     * Check if current user has viewed a specific post
     */
    @GetMapping("/post/{postId}/viewed")
    @Operation(summary = "Check if post was viewed", description = "Checks if the authenticated user has viewed a specific post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "View status retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = ViewedResponse.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<ViewedResponse> hasViewedPost(
            @Parameter(description = "ID of the post", required = true)
            @PathVariable String postId) {
        return currentUserService.getCurrentUserId()
                                 .flatMap(userId -> viewTrackingService.hasUserViewedPost(userId, postId))
                                 .map(ViewedResponse::new)
                                 .onErrorReturn(new ViewedResponse(false))
                                 .doOnError(e -> log.error("Failed to check if post was viewed: {}", e.getMessage()));
    }
}