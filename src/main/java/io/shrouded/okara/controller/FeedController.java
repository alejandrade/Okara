package io.shrouded.okara.controller;

import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.User;
import io.shrouded.okara.service.CurrentUserService;
import io.shrouded.okara.service.FeedService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/feed")
@RequiredArgsConstructor
@Slf4j
public class FeedController {
    
    private final FeedService feedService;
    private final CurrentUserService currentUserService;
    
    @PostMapping("/post")
    public ResponseEntity<?> createPost(@RequestBody Map<String, Object> postData) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            String content = (String) postData.get("content");
            @SuppressWarnings("unchecked")
            List<String> imageUrls = (List<String>) postData.get("imageUrls");
            String videoUrl = (String) postData.get("videoUrl");
            
            Feed post = feedService.createPost(currentUser.getId(), content, imageUrls, videoUrl);
            
            return ResponseEntity.ok(post);
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{postId}/comment")
    public ResponseEntity<?> createComment(
            @PathVariable String postId,
            @RequestBody Map<String, String> commentData) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            String content = commentData.get("content");
            
            Feed comment = feedService.createComment(currentUser.getId(), postId, content);
            
            return ResponseEntity.ok(comment);
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{postId}/like")
    public ResponseEntity<?> likePost(
            @PathVariable String postId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            Feed post = feedService.likePost(currentUser.getId(), postId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Post liked successfully",
                "post", post
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{postId}/dislike")
    public ResponseEntity<?> dislikePost(
            @PathVariable String postId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            Feed post = feedService.dislikePost(currentUser.getId(), postId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Post disliked successfully",
                "post", post
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{postId}/retweet")
    public ResponseEntity<?> retweet(
            @PathVariable String postId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            Feed post = feedService.retweetPost(currentUser.getId(), postId);
            
            boolean isRetweeted = post.getRetweetedBy().contains(currentUser.getId());
            String message = isRetweeted ? "Retweeted successfully" : "Retweet removed successfully";
            
            return ResponseEntity.ok(Map.of(
                "message", message,
                "post", post
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @PostMapping("/{postId}/quote")
    public ResponseEntity<?> quoteRetweet(
            @PathVariable String postId,
            @RequestBody Map<String, String> quoteData) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            String comment = quoteData.get("comment");
            
            Feed quoteRetweet = feedService.quoteRetweet(currentUser.getId(), postId, comment);
            
            return ResponseEntity.ok(Map.of(
                "message", "Quote tweet created successfully",
                "quoteRetweet", quoteRetweet
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/main")
    public ResponseEntity<Page<Feed>> getMainFeed(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Feed> feed = feedService.getMainFeed(pageable);
        
        return ResponseEntity.ok(feed);
    }
    
    @GetMapping("/timeline")
    public ResponseEntity<?> getTimeline(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            Pageable pageable = PageRequest.of(page, size);
            Page<Feed> timeline = feedService.getTimeline(currentUser.getId(), pageable);
            
            return ResponseEntity.ok(timeline);
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
    
    @GetMapping("/user/{userId}")
    public ResponseEntity<Page<Feed>> getUserFeed(
            @PathVariable String userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<Feed> userFeed = feedService.getUserFeed(userId, pageable);
        
        return ResponseEntity.ok(userFeed);
    }
    
    @GetMapping("/{postId}")
    public ResponseEntity<?> getPost(@PathVariable String postId) {
        return feedService.findById(postId)
                .map(post -> ResponseEntity.ok(post))
                .orElse(ResponseEntity.notFound().build());
    }
    
    @GetMapping("/{postId}/comments")
    public ResponseEntity<List<Feed>> getComments(@PathVariable String postId) {
        List<Feed> comments = feedService.getComments(postId);
        return ResponseEntity.ok(comments);
    }
    
    @DeleteMapping("/{postId}")
    public ResponseEntity<?> deletePost(
            @PathVariable String postId) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            
            Feed deletedPost = feedService.deletePost(currentUser.getId(), postId);
            
            return ResponseEntity.ok(Map.of(
                "message", "Post deleted successfully",
                "post", deletedPost
            ));
            
        } catch (Exception e) {
            return ResponseEntity.status(400).body(Map.of(
                "error", e.getMessage()
            ));
        }
    }
}