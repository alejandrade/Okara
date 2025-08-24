package io.shrouded.okara.controller;

import io.shrouded.okara.dto.feed.CreateCommentRequest;
import io.shrouded.okara.dto.feed.CreatePostRequest;
import io.shrouded.okara.dto.feed.CrossPostRequest;
import io.shrouded.okara.dto.feed.FeedDto;
import io.shrouded.okara.dto.feed.QuoteRetweetRequest;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.mapper.FeedMapper;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.FeedService;
import io.shrouded.okara.service.PersonalFeedService;
import io.shrouded.okara.service.ChatroomService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;
import reactor.core.publisher.Mono;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Feed", description = "Social feed management and interaction endpoints")
@SecurityRequirement(name = "bearerAuth")
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserService currentUserService;
    private final PersonalFeedService personalFeedService;
    private final FeedMapper feedMapper;
    private final ChatroomService chatroomService;

    @PostMapping("/post")
    @Operation(summary = "Create post", description = "Creates a new post in the feed with optional media attachments")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid post data",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> createPost(
            @Parameter(description = "Post creation request", required = true)
            @Valid @RequestBody CreatePostRequest request) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> {
                                     // Validate that all chatrooms exist
                                     return chatroomService.validateChatroomsExist(request.chatroomIds())
                                             .then(feedService.createPost(currentUser.getId(),
                                                                          request.content(),
                                                                          request.imageUrls(),
                                                                          request.videoUrl(),
                                                                          request.chatroomIds()))
                                             .map(feedMapper::toFeedDto);
                                 });
    }

    @PostMapping("/{postId}/cross-post")
    @Operation(summary = "Cross-post to chatrooms", description = "Share an existing post to additional chatrooms")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post cross-posted successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid cross-post data",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> crossPost(
            @Parameter(description = "ID of the post to cross-post", required = true)
            @PathVariable String postId,
            @Parameter(description = "Cross-post request with chatroom IDs", required = true)
            @Valid @RequestBody CrossPostRequest request) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser -> {
                                     // Validate that all chatrooms exist
                                     return chatroomService.validateChatroomsExist(request.chatroomIds())
                                             .then(feedService.crossPost(currentUser.getId(),
                                                                         postId,
                                                                         request.chatroomIds()))
                                             .map(feedMapper::toFeedDto);
                                 });
    }

    @PostMapping("/{postId}/comment")
    @Operation(summary = "Comment on post", description = "Add a comment to an existing post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comment created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "400", description = "Invalid comment data",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> createComment(
            @Parameter(description = "ID of the post to comment on", required = true)
            @PathVariable String postId,
            @Parameter(description = "Comment creation request", required = true)
            @RequestBody CreateCommentRequest request) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.createComment(currentUser.getId(),
                                                                            postId,
                                                                            request.content())
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/like")
    @Operation(summary = "Like post", description = "Like or unlike a post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post liked successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> likePost(
            @Parameter(description = "ID of the post to like", required = true)
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.likePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/dislike")
    @Operation(summary = "Dislike post", description = "Dislike or remove dislike from a post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post disliked successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> dislikePost(
            @Parameter(description = "ID of the post to dislike", required = true)
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.dislikePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/retweet")
    @Operation(summary = "Retweet post", description = "Retweet an existing post to share with followers")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post retweeted successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> retweet(
            @Parameter(description = "ID of the post to retweet", required = true)
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.retweetPost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/quote")
    @Operation(summary = "Quote retweet", description = "Quote retweet with additional commentary")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Quote retweet created successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> quoteRetweet(
            @Parameter(description = "ID of the post to quote retweet", required = true)
            @PathVariable String postId,
            @Parameter(description = "Quote retweet request with comment", required = true)
            @RequestBody QuoteRetweetRequest request) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.quoteRetweet(currentUser.getId(),
                                                                           postId,
                                                                           request.comment())
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @GetMapping("/main")
    @Operation(summary = "Get main feed", description = "Retrieves the user's main feed with posts from followed users and joined chatrooms")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Feed retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = FeedDto.class))),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<FeedDto>> getMainFeed(
            @Parameter(description = "Maximum number of posts to return", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "ID of the last post from previous page for pagination")
            @RequestParam(required = false) String sinceId) {
        log.info("🎯 Main feed request - limit: {}, sinceId: {}", limit, sinceId);

        return currentUserService.getCurrentUser()
                                 .doOnNext(user -> log.info("🎯 Got current user: {}", user.getId()))
                                 .flatMap(currentUser -> {
                                     // Get user's personal feed (simple chronological)
                                     log.info("🎯 Getting user's personal feed");
                                     return personalFeedService.getPersonalFeed(currentUser.getId(), limit, sinceId)
                                                               .doOnNext(feedItems -> log.info(
                                                                       "🎯 Found {} items in personal feed",
                                                                       feedItems.size()))
                                                               .map(feedItems -> {
                                                                   List<FeedDto> feedDtos = feedItems.stream()
                                                                                                     .map(feedMapper::convertToFeed)
                                                                                                     .map(feedMapper::toFeedDto)
                                                                                                     .toList();
                                                                   log.info("🎯 Converted to {} DTOs", feedDtos.size());
                                                                   return feedDtos;
                                                               });
                                 })
                                 .doOnError(e -> log.error("🎯 Error in main feed chain: {}", e.getMessage(), e))
                                 .doFinally(signal -> log.info("🎯 Main feed request completed with signal: {}",
                                                               signal));
    }

    @GetMapping("/chatroom/{chatroomId}")
    @Operation(summary = "Get chatroom feed", description = "Retrieves posts from a specific chatroom")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Chatroom feed retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Chatroom not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<FeedDto>> getChatroomFeed(
            @Parameter(description = "ID of the chatroom", required = true)
            @PathVariable String chatroomId,
            @Parameter(description = "Maximum number of posts to return", example = "20")
            @RequestParam(defaultValue = "20") int limit,
            @Parameter(description = "ID of the last post from previous page for pagination")
            @RequestParam(required = false) String sinceId) {
        log.info("🎯 Chatroom feed request - chatroomId: {}, limit: {}, sinceId: {}", chatroomId, limit, sinceId);

        return feedService.getChatroomFeed(chatroomId, limit, sinceId)
                          .doOnNext(feeds -> log.info("🎯 Found {} feeds in chatroom {}", feeds.size(), chatroomId))
                          .map(feeds -> feeds.stream()
                                             .map(feedMapper::toFeedDto)
                                             .toList())
                          .doOnError(e -> log.error("🎯 Error in chatroom feed chain: {}", e.getMessage(), e))
                          .doFinally(signal -> log.info("🎯 Chatroom feed request completed with signal: {}", signal));
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get user feed", description = "Retrieves posts from a specific user's profile")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "User feed retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "User not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<FeedDto>> getUserFeed(
            @Parameter(description = "ID of the user", required = true)
            @PathVariable String userId,
            @Parameter(description = "Page number for pagination", example = "0")
            @RequestParam(defaultValue = "0") int page,
            @Parameter(description = "Number of posts per page", example = "20")
            @RequestParam(defaultValue = "20") int size) {

        return feedService.getUserFeed(userId)
                          .collectList()
                          .map(userFeed -> {
                              // Apply pagination manually since we're using the old method
                              int fromIndex = page * size;
                              int toIndex = Math.min(fromIndex + size, userFeed.size());

                              if (fromIndex >= userFeed.size()) {
                                  return Collections.emptyList();
                              }

                              return userFeed.subList(fromIndex, toIndex)
                                            .stream()
                                            .map(feedMapper::toFeedDto)
                                            .toList();
                          });
    }

    @GetMapping("/{postId}")
    @Operation(summary = "Get post by ID", description = "Retrieves a specific post by its ID")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post found",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> getPost(
            @Parameter(description = "ID of the post", required = true)
            @PathVariable String postId) {
        return feedService.findById(postId)
                          .map(feedMapper::toFeedDto)
                          .switchIfEmpty(Mono.error(OkaraException.notFound("post")));
    }

    @GetMapping("/{postId}/comments")
    @Operation(summary = "Get post comments", description = "Retrieves all comments for a specific post")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Comments retrieved successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(type = "array", implementation = FeedDto.class))),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<List<FeedDto>> getComments(
            @Parameter(description = "ID of the post", required = true)
            @PathVariable String postId) {
        return feedService.getComments(postId)
                          .collectList()
                          .map(comments -> comments.stream()
                                                  .map(feedMapper::toFeedDto)
                                                  .toList());
    }

    @DeleteMapping("/{postId}")
    @Operation(summary = "Delete post", description = "Deletes a post (only the post owner can delete)")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Post deleted successfully",
                content = @Content(mediaType = "application/json", schema = @Schema(implementation = FeedDto.class))),
        @ApiResponse(responseCode = "403", description = "Not authorized to delete this post",
                content = @Content),
        @ApiResponse(responseCode = "404", description = "Post not found",
                content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized",
                content = @Content)
    })
    public Mono<FeedDto> deletePost(
            @Parameter(description = "ID of the post to delete", required = true)
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.deletePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

}