package io.shrouded.okara.service;

import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.User;
import io.shrouded.okara.repository.FeedRepository;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {
    
    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    
    public Feed createPost(String authorId, String content, List<String> imageUrls, String videoUrl) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Feed post = new Feed(authorId, author.getUsername(), content, Feed.FeedType.POST);
        post.setAuthorDisplayName(author.getDisplayName());
        post.setAuthorProfileImageUrl(author.getProfileImageUrl());
        post.setImageUrls(imageUrls);
        post.setVideoUrl(videoUrl);
        
        // Extract hashtags and mentions
        post.setHashtags(extractHashtags(content));
        post.setMentions(extractMentions(content));
        
        Feed savedPost = feedRepository.save(post);
        
        // Update user's post count
        author.setPostsCount(author.getPostsCount() + 1);
        author.setUpdatedAt(LocalDateTime.now());
        userRepository.save(author);
        
        return savedPost;
    }
    
    public Feed createComment(String authorId, String parentId, String content) {
        User author = userRepository.findById(authorId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Feed parentPost = feedRepository.findById(parentId)
                .orElseThrow(() -> new RuntimeException("Parent post not found"));
        
        Feed comment = new Feed(authorId, author.getUsername(), content, Feed.FeedType.COMMENT);
        comment.setAuthorDisplayName(author.getDisplayName());
        comment.setAuthorProfileImageUrl(author.getProfileImageUrl());
        comment.setParentId(parentId);
        comment.setRootId(parentPost.getRootId() != null ? parentPost.getRootId() : parentId);
        
        // Extract hashtags and mentions
        comment.setHashtags(extractHashtags(content));
        comment.setMentions(extractMentions(content));
        
        Feed savedComment = feedRepository.save(comment);
        
        // Update parent post's comment count and engagement score
        parentPost.setCommentsCount(parentPost.getCommentsCount() + 1);
        updateDistinctCommentersCount(parentPost);
        calculateAndUpdateEngagementScore(parentPost);
        parentPost.setUpdatedAt(LocalDateTime.now());
        feedRepository.save(parentPost);
        
        return savedComment;
    }
    
    public Feed likePost(String userId, String postId) {
        Feed post = feedRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getLikedBy().contains(userId)) {
            // Remove from disliked if exists
            if (post.getDislikedBy().contains(userId)) {
                post.getDislikedBy().remove(userId);
                post.setDislikesCount(post.getDislikesCount() - 1);
            }
            
            post.getLikedBy().add(userId);
            post.setLikesCount(post.getLikesCount() + 1);
            calculateAndUpdateEngagementScore(post);
            post.setUpdatedAt(LocalDateTime.now());
        }
        
        return feedRepository.save(post);
    }
    
    public Feed dislikePost(String userId, String postId) {
        Feed post = feedRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getDislikedBy().contains(userId)) {
            // Remove from liked if exists
            if (post.getLikedBy().contains(userId)) {
                post.getLikedBy().remove(userId);
                post.setLikesCount(post.getLikesCount() - 1);
            }
            
            post.getDislikedBy().add(userId);
            post.setDislikesCount(post.getDislikesCount() + 1);
            calculateAndUpdateEngagementScore(post);
            post.setUpdatedAt(LocalDateTime.now());
        }
        
        return feedRepository.save(post);
    }
    
    public Feed retweetPost(String userId, String postId) {
        Feed post = feedRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        // Toggle retweet behavior (like/unlike)
        if (post.getRetweetedBy().contains(userId)) {
            // User already retweeted, so unretweet (remove)
            post.getRetweetedBy().remove(userId);
            post.setRetweetsCount(post.getRetweetsCount() - 1);
        } else {
            // User hasn't retweeted, so add retweet
            post.getRetweetedBy().add(userId);
            post.setRetweetsCount(post.getRetweetsCount() + 1);
        }
        
        // Recalculate engagement score
        calculateAndUpdateEngagementScore(post);
        post.setUpdatedAt(LocalDateTime.now());
        
        return feedRepository.save(post);
    }
    
    public Feed quoteRetweet(String userId, String originalPostId, String comment) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        Feed originalPost = feedRepository.findById(originalPostId)
                .orElseThrow(() -> new RuntimeException("Original post not found"));
        
        Feed quoteRetweet = new Feed(userId, user.getUsername(), comment, Feed.FeedType.QUOTE_TWEET);
        quoteRetweet.setAuthorDisplayName(user.getDisplayName());
        quoteRetweet.setAuthorProfileImageUrl(user.getProfileImageUrl());
        quoteRetweet.setOriginalPostId(originalPostId);
        quoteRetweet.setQuoteTweetComment(comment);
        
        // Extract hashtags and mentions from comment
        quoteRetweet.setHashtags(extractHashtags(comment));
        quoteRetweet.setMentions(extractMentions(comment));
        
        Feed savedQuoteRetweet = feedRepository.save(quoteRetweet);
        
        // Update original post's retweet count
        if (!originalPost.getRetweetedBy().contains(userId)) {
            originalPost.getRetweetedBy().add(userId);
            originalPost.setRetweetsCount(originalPost.getRetweetsCount() + 1);
            originalPost.setUpdatedAt(LocalDateTime.now());
            feedRepository.save(originalPost);
        }
        
        return savedQuoteRetweet;
    }
    
    public Page<Feed> getMainFeed(Pageable pageable) {
        // Use engagement-based algorithm for main feed
        return feedRepository.findByParentIdIsNullAndIsDeletedFalseOrderByBaseEngagementScoreDescCreatedAtDesc(pageable);
    }
    
    public Page<Feed> getMainFeedChronological(Pageable pageable) {
        // Fallback method for chronological sorting if needed
        return feedRepository.findByParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(pageable);
    }
    
    public Page<Feed> getUserFeed(String userId, Pageable pageable) {
        return feedRepository.findByAuthorIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(userId, pageable);
    }
    
    public Page<Feed> getTimeline(String userId, Pageable pageable) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        List<String> followingIds = user.getFollowing();
        followingIds.add(userId); // Include user's own posts
        
        return feedRepository.findTimelineByFollowing(followingIds, pageable);
    }
    
    public List<Feed> getComments(String postId) {
        return feedRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(postId);
    }
    
    public Optional<Feed> findById(String id) {
        return feedRepository.findById(id);
    }
    
    public Feed deletePost(String userId, String postId) {
        Feed post = feedRepository.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post not found"));
        
        if (!post.getAuthorId().equals(userId)) {
            throw new RuntimeException("Unauthorized to delete this post");
        }
        
        post.setDeleted(true);
        post.setUpdatedAt(LocalDateTime.now());
        
        // If this is a comment (has a parent), fetch both posts in a single query and update parent
        if (post.getParentId() != null) {
            List<String> ids = List.of(postId, post.getParentId());
            List<Feed> posts = feedRepository.findAllById(ids);
            
            Optional<Feed> parentPostOpt = posts.stream()
                    .filter(p -> p.getId().equals(post.getParentId()))
                    .findFirst();
                    
            if (parentPostOpt.isPresent()) {
                Feed parentPost = parentPostOpt.get();
                parentPost.setCommentsCount(Math.max(0, parentPost.getCommentsCount() - 1));
                parentPost.setUpdatedAt(LocalDateTime.now());
                
                // Recalculate engagement score for parent post
                calculateAndUpdateEngagementScore(parentPost);
                
                // Save both posts in batch
                feedRepository.saveAll(List.of(post, parentPost));
                return post;
            }
        }
        
        return feedRepository.save(post);
    }
    
    private List<String> extractHashtags(String content) {
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(content);
        return matcher.results()
                .map(matchResult -> matchResult.group().substring(1)) // Remove #
                .toList();
    }
    
    private List<String> extractMentions(String content) {
        Pattern pattern = Pattern.compile("@\\w+");
        Matcher matcher = pattern.matcher(content);
        return matcher.results()
                .map(matchResult -> matchResult.group().substring(1)) // Remove @
                .toList();
    }
    
    /**
     * Calculate base engagement score for a post
     * Formula: (likes * 1.0) + (retweets * 1.5) + (comment_score_with_diminishing_returns) + time_decay
     */
    private void calculateAndUpdateEngagementScore(Feed post) {
        double score = 0.0;
        
        // Likes: 1 point each
        score += post.getLikesCount() * 1.0;
        
        // Retweets: 1.5 points each (higher weight since they're more valuable)
        score += post.getRetweetsCount() * 1.5;
        
        // Comments with diminishing returns per user
        score += calculateCommentScore(post);
        
        // Time decay: newer posts get slight boost (max 10 points, decays over 7 days)
        score += calculateTimeDecay(post.getCreatedAt());
        
        post.setBaseEngagementScore(score);
        log.debug("Updated engagement score for post {}: {} (likes: {}, retweets: {}, comments: {})", 
                post.getId(), score, post.getLikesCount(), post.getRetweetsCount(), post.getDistinctCommentersCount());
    }
    
    /**
     * Calculate comment score with diminishing returns per user
     * First comment from user: +2.0, Second: +1.0, Third+: +0.5 each
     */
    private double calculateCommentScore(Feed post) {
        // Get all comments for this post to count per-user contributions
        List<Feed> comments = feedRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(
            post.getId() != null ? post.getId() : post.getRootId()
        );
        
        // Count comments per user with diminishing returns
        var userCommentCounts = new java.util.HashMap<String, Integer>();
        for (Feed comment : comments) {
            userCommentCounts.merge(comment.getAuthorId(), 1, Integer::sum);
        }
        
        double commentScore = 0.0;
        Set<String> distinctCommenters = new HashSet<>();
        
        for (var entry : userCommentCounts.entrySet()) {
            String userId = entry.getKey();
            int commentCount = entry.getValue();
            distinctCommenters.add(userId);
            
            // Diminishing returns: 2.0 + 1.0 + 0.5 + 0.5 + ... (max ~4 points per user)
            if (commentCount >= 1) commentScore += 2.0; // First comment
            if (commentCount >= 2) commentScore += 1.0; // Second comment  
            if (commentCount >= 3) commentScore += (commentCount - 2) * 0.5; // 3+ comments
        }
        
        // Update distinct commenters count
        post.setDistinctCommentersCount(distinctCommenters.size());
        
        return commentScore;
    }
    
    /**
     * Calculate time decay factor (0-10 points)
     * Newer posts get higher boost, decays over 7 days
     */
    private double calculateTimeDecay(LocalDateTime createdAt) {
        long hoursOld = ChronoUnit.HOURS.between(createdAt, LocalDateTime.now());
        
        // Fresh posts (0-24h): 8-10 points
        if (hoursOld <= 24) return 10.0 - (hoursOld / 24.0) * 2.0;
        
        // Recent posts (1-7 days): 2-8 points  
        if (hoursOld <= 168) return 8.0 - ((hoursOld - 24) / 144.0) * 6.0;
        
        // Old posts (7+ days): 0-2 points
        return Math.max(0, 2.0 - ((hoursOld - 168) / 168.0) * 2.0);
    }
    
    /**
     * Update distinct commenters count for a post
     */
    private void updateDistinctCommentersCount(Feed post) {
        List<Feed> comments = feedRepository.findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(post.getId());
        Set<String> distinctCommenters = new HashSet<>();
        
        for (Feed comment : comments) {
            distinctCommenters.add(comment.getAuthorId());
        }
        
        post.setDistinctCommentersCount(distinctCommenters.size());
    }
    
    /**
     * Recalculate engagement scores for all existing posts
     * Call this once to migrate existing data
     */
    public void recalculateAllEngagementScores() {
        log.info("Starting engagement score recalculation for all posts...");
        
        List<Feed> allPosts = feedRepository.findAll();
        int processed = 0;
        
        for (Feed post : allPosts) {
            if (post.getParentId() == null) { // Only recalculate for main posts, not comments
                calculateAndUpdateEngagementScore(post);
                feedRepository.save(post);
                processed++;
                
                if (processed % 100 == 0) {
                    log.info("Processed {} posts", processed);
                }
            }
        }
        
        log.info("Completed engagement score recalculation for {} posts", processed);
    }
}