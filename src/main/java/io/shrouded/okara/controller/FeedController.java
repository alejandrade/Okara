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

import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {

    private final FeedService feedService;
    private final CurrentUserService currentUserService;
    private final PersonalFeedService personalFeedService;
    private final FeedMapper feedMapper;
    private final ChatroomService chatroomService;

    @PostMapping("/post")
    public Mono<FeedDto> createPost(@Valid @RequestBody CreatePostRequest request) {
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
    public Mono<FeedDto> crossPost(
            @PathVariable String postId,
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
    public Mono<FeedDto> createComment(
            @PathVariable String postId,
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
    public Mono<FeedDto> likePost(@PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.likePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/dislike")
    public Mono<FeedDto> dislikePost(@PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.dislikePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/retweet")
    public Mono<FeedDto> retweet(@PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.retweetPost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

    @PostMapping("/{postId}/quote")
    public Mono<FeedDto> quoteRetweet(
            @PathVariable String postId,
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
    public Mono<List<FeedDto>> getMainFeed(
            @RequestParam(defaultValue = "20") int limit,
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
    public Mono<List<FeedDto>> getChatroomFeed(
            @PathVariable String chatroomId,
            @RequestParam(defaultValue = "20") int limit,
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
    public Mono<List<FeedDto>> getUserFeed(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
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
    public Mono<FeedDto> getPost(@PathVariable String postId) {
        return feedService.findById(postId)
                          .map(feedMapper::toFeedDto)
                          .switchIfEmpty(Mono.error(OkaraException.notFound("post")));
    }

    @GetMapping("/{postId}/comments")
    public Mono<List<FeedDto>> getComments(@PathVariable String postId) {
        return feedService.getComments(postId)
                          .collectList()
                          .map(comments -> comments.stream()
                                                  .map(feedMapper::toFeedDto)
                                                  .toList());
    }

    @DeleteMapping("/{postId}")
    public Mono<FeedDto> deletePost(@PathVariable String postId) {
        return currentUserService.getCurrentUser()
                                 .flatMap(currentUser ->
                                                  feedService.deletePost(currentUser.getId(), postId)
                                                             .map(feedMapper::toFeedDto)
                                 );
    }

}