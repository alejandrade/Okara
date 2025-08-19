package io.shrouded.okara.controller;

import io.shrouded.okara.dto.FeedDto;
import io.shrouded.okara.exception.OkaraException;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.DtoMappingService;
import io.shrouded.okara.service.FeedService;
import io.shrouded.okara.service.PersonalFeedService;
import io.shrouded.okara.service.FeedMappingService;
import io.shrouded.okara.model.UserFeed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    
    private final FeedService feedService;
    private final CurrentUserService currentUserService;
    private final PersonalFeedService personalFeedService;
    private final FeedMappingService feedMappingService;
    private final DtoMappingService dtoMappingService;
    
    @PostMapping("/post")
    public Mono<ResponseEntity<FeedDto>> createPost(@RequestBody Map<String, Object> postData) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                String content = (String) postData.get("content");
                @SuppressWarnings("unchecked")
                List<String> imageUrls = (List<String>) postData.get("imageUrls");
                String videoUrl = (String) postData.get("videoUrl");
                
                return feedService.createPost(currentUser.getId(), content, imageUrls, videoUrl)
                    .map(post -> ResponseEntity.ok(dtoMappingService.toFeedDto(post)));
            });
    }
    
    @PostMapping("/{postId}/comment")
    public Mono<ResponseEntity<FeedDto>> createComment(
            @PathVariable String postId,
            @RequestBody Map<String, String> commentData) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                String content = commentData.get("content");
                
                return feedService.createComment(currentUser.getId(), postId, content)
                    .map(comment -> ResponseEntity.ok(dtoMappingService.toFeedDto(comment)));
            });
    }
    
    @PostMapping("/{postId}/like")
    public Mono<ResponseEntity<FeedDto>> likePost(
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                feedService.likePost(currentUser.getId(), postId)
                    .map(post -> ResponseEntity.ok(dtoMappingService.toFeedDto(post)))
            );
    }
    
    @PostMapping("/{postId}/dislike")
    public Mono<ResponseEntity<FeedDto>> dislikePost(
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                feedService.dislikePost(currentUser.getId(), postId)
                    .map(post -> ResponseEntity.ok(dtoMappingService.toFeedDto(post)))
            );
    }
    
    @PostMapping("/{postId}/retweet")
    public Mono<ResponseEntity<FeedDto>> retweet(
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                feedService.retweetPost(currentUser.getId(), postId)
                    .map(post -> ResponseEntity.ok(dtoMappingService.toFeedDto(post)))
            );
    }
    
    @PostMapping("/{postId}/quote")
    public Mono<ResponseEntity<FeedDto>> quoteRetweet(
            @PathVariable String postId,
            @RequestBody Map<String, String> quoteData) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> {
                String comment = quoteData.get("comment");
                
                return feedService.quoteRetweet(currentUser.getId(), postId, comment)
                    .map(quoteRetweet -> ResponseEntity.ok(dtoMappingService.toFeedDto(quoteRetweet)));
            });
    }
    
    @GetMapping("/main")
    public Mono<ResponseEntity<List<FeedDto>>> getMainFeed(
            @RequestParam(defaultValue = "20") int limit,
            @RequestParam(required = false) String sinceId) {
        log.info("🎯 Main feed request - limit: {}, sinceId: {}", limit, sinceId);
        
        return currentUserService.getCurrentUser()
            .doOnNext(user -> log.info("🎯 Got current user: {}", user.getId()))
            .flatMap(currentUser -> {
                // Get user's personal feed (simple chronological)
                log.info("🎯 Getting user's personal feed");
                return personalFeedService.getPersonalFeed(currentUser.getId(), limit, sinceId)
                    .doOnNext(feedItems -> log.info("🎯 Found {} items in personal feed", feedItems.size()))
                    .map(feedItems -> {
                        List<FeedDto> feedDtos = feedItems.stream()
                            .map(feedMappingService::convertToFeed)
                            .filter(feed -> feed != null)
                            .map(dtoMappingService::toFeedDto)
                            .toList();
                        log.info("🎯 Converted to {} DTOs", feedDtos.size());
                        return ResponseEntity.ok(feedDtos);
                    });
            })
            .doOnError(e -> log.error("🎯 Error in main feed chain: {}", e.getMessage(), e))
            .doFinally(signal -> log.info("🎯 Main feed request completed with signal: {}", signal));
    }
    
    @GetMapping("/user/{userId}")
    public Mono<ResponseEntity<List<FeedDto>>> getUserFeed(
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
                    return ResponseEntity.ok(new ArrayList<FeedDto>());
                }
                
                List<FeedDto> paginatedFeed = userFeed.subList(fromIndex, toIndex)
                    .stream()
                    .map(dtoMappingService::toFeedDto)
                    .toList();
                return ResponseEntity.ok(paginatedFeed);
            });
    }
    
    @GetMapping("/{postId}")
    public Mono<ResponseEntity<FeedDto>> getPost(@PathVariable String postId) {
        return feedService.findById(postId)
                .map(post -> ResponseEntity.ok(dtoMappingService.toFeedDto(post)))
                .switchIfEmpty(Mono.error(OkaraException.notFound("post")));
    }
    
    @GetMapping("/{postId}/comments")
    public Mono<ResponseEntity<List<FeedDto>>> getComments(@PathVariable String postId) {
        return feedService.getComments(postId)
            .collectList()
            .map(comments -> {
                List<FeedDto> commentDtos = comments.stream()
                    .map(dtoMappingService::toFeedDto)
                    .toList();
                return ResponseEntity.ok(commentDtos);
            });
    }
    
    @DeleteMapping("/{postId}")
    public Mono<ResponseEntity<FeedDto>> deletePost(
            @PathVariable String postId) {
        return currentUserService.getCurrentUser()
            .flatMap(currentUser -> 
                feedService.deletePost(currentUser.getId(), postId)
                    .map(deletedPost -> ResponseEntity.ok(dtoMappingService.toFeedDto(deletedPost)))
            );
    }

}