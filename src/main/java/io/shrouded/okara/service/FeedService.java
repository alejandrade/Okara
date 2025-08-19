package io.shrouded.okara.service;

import com.google.cloud.Timestamp;
import io.shrouded.okara.enums.FeedType;
import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.User;
import io.shrouded.okara.repository.FeedRepository;
import io.shrouded.okara.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class FeedService {

    private final FeedRepository feedRepository;
    private final UserRepository userRepository;
    private final FeedEventPublisher feedEventPublisher;

    /**
     * Create a main post (reactive)
     */
    public Mono<Feed> createPost(String authorId, String content, List<String> imageUrls, String videoUrl) {
        return userRepository.findById(authorId)
                             .switchIfEmpty(Mono.error(new RuntimeException("User not found")))
                             .flatMap(author -> {
                                 Feed post = new Feed(authorId, author.getUsername(), content, FeedType.POST);
                                 post.setAuthorDisplayName(author.getDisplayName());
                                 post.setAuthorProfileImageUrl(author.getProfileImageUrl());
                                 post.setImageUrls(imageUrls);
                                 post.setVideoUrl(videoUrl);
                                 post.setHashtags(extractHashtags(content));
                                 post.setMentions(extractMentions(content));
                                 return feedRepository.save(post)
                                                      .flatMap(savedPost ->
                                                                       userRepository.save(incrementPosts(author))
                                                                                     .thenReturn(savedPost)
                                                      )
                                                      .flatMap(this::calculateAndUpdateEngagementScore) // compute baseEngagementScore
                                                      .flatMap(feedRepository::save)
                                                      .doOnSuccess(savedPost -> {
                                                          try {
                                                              feedEventPublisher.publishPostCreated(savedPost);
                                                          } catch (Exception e) {
                                                              log.error(
                                                                      "Failed to publish post created event for {}: {}",
                                                                      savedPost.getId(),
                                                                      e.getMessage());
                                                          }
                                                      });
                             });
    }

    /**
     * Create a comment (reactive)
     */
    public Mono<Feed> createComment(String authorId, String parentId, String content) {
        return Mono.zip(
                           userRepository.findById(authorId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User not found"))),
                           feedRepository.findById(parentId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Parent post not found")))
                   )
                   .flatMap(tuple -> {
                       User author = tuple.getT1();
                       Feed parentPost = tuple.getT2();

                       Feed comment = new Feed(authorId, author.getUsername(), content, FeedType.COMMENT);
                       comment.setAuthorDisplayName(author.getDisplayName());
                       comment.setAuthorProfileImageUrl(author.getProfileImageUrl());
                       comment.setParentId(parentId);
                       comment.setRootId(parentPost.getRootId() != null ? parentPost.getRootId() : parentId);
                       comment.setHashtags(extractHashtags(content));
                       comment.setMentions(extractMentions(content));

                       return feedRepository.save(comment)
                                            .flatMap(savedComment ->
                                                             // Update parent post counts & engagement
                                                             feedRepository.findById(parentId)
                                                                           .flatMap(p -> {
                                                                               p.setCommentsCount(p.getCommentsCount() + 1);
                                                                               p.setUpdatedAt(Timestamp.now());
                                                                               return updateDistinctCommentersCount(p)
                                                                                       .flatMap(this::calculateAndUpdateEngagementScore)
                                                                                       .flatMap(feedRepository::save)
                                                                                       .doOnSuccess(updated -> {
                                                                                           try {
                                                                                               feedEventPublisher.publishPostUpdated(
                                                                                                       updated);
                                                                                           } catch (Exception e) {
                                                                                               log.error(
                                                                                                       "Failed to publish post updated event for {}: {}",
                                                                                                       updated.getId(),
                                                                                                       e.getMessage());
                                                                                           }
                                                                                       })
                                                                                       .thenReturn(savedComment);
                                                                           })
                                            );
                   });
    }

    /**
     * Like post (toggle add-only)
     */
    public Mono<Feed> likePost(String userId, String postId) {
        return feedRepository.findById(postId)
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                             .flatMap(post -> {
                                 boolean mutated = false;
                                 if (!post.getLikedBy().contains(userId)) {
                                     if (post.getDislikedBy().remove(userId)) {
                                         post.setDislikesCount(Math.max(0, post.getDislikesCount() - 1));
                                     }
                                     post.getLikedBy().add(userId);
                                     post.setLikesCount(post.getLikesCount() + 1);
                                     mutated = true;
                                 }
                                 if (!mutated) {
                                     return Mono.just(post);
                                 }
                                 post.setUpdatedAt(Timestamp.now());
                                 return calculateAndUpdateEngagementScore(post)
                                         .flatMap(feedRepository::save);
                             });
    }

    /**
     * Dislike post (toggle add-only)
     */
    public Mono<Feed> dislikePost(String userId, String postId) {
        return feedRepository.findById(postId)
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                             .flatMap(post -> {
                                 boolean mutated = false;
                                 if (!post.getDislikedBy().contains(userId)) {
                                     if (post.getLikedBy().remove(userId)) {
                                         post.setLikesCount(Math.max(0, post.getLikesCount() - 1));
                                     }
                                     post.getDislikedBy().add(userId);
                                     post.setDislikesCount(post.getDislikesCount() + 1);
                                     mutated = true;
                                 }
                                 if (!mutated) {
                                     return Mono.just(post);
                                 }
                                 post.setUpdatedAt(Timestamp.now());
                                 return calculateAndUpdateEngagementScore(post)
                                         .flatMap(feedRepository::save);
                             });
    }

    /**
     * Retweet (toggle)
     */
    public Mono<Feed> retweetPost(String userId, String postId) {
        return feedRepository.findById(postId)
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                             .flatMap(post -> {
                                 if (post.getRetweetedBy().contains(userId)) {
                                     post.getRetweetedBy().remove(userId);
                                     post.setRetweetsCount(Math.max(0, post.getRetweetsCount() - 1));
                                 } else {
                                     post.getRetweetedBy().add(userId);
                                     post.setRetweetsCount(post.getRetweetsCount() + 1);
                                 }
                                 post.setUpdatedAt(Timestamp.now());
                                 return calculateAndUpdateEngagementScore(post)
                                         .flatMap(feedRepository::save);
                             });
    }

    /**
     * Quote-retweet
     */
    public Mono<Feed> quoteRetweet(String userId, String originalPostId, String comment) {
        return Mono.zip(
                           userRepository.findById(userId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("User not found"))),
                           feedRepository.findById(originalPostId)
                                         .switchIfEmpty(Mono.error(new RuntimeException("Original post not found")))
                   )
                   .flatMap(tuple -> {
                       User user = tuple.getT1();
                       Feed originalPost = tuple.getT2();

                       Feed qt = new Feed(userId, user.getUsername(), comment, FeedType.QUOTE_TWEET);
                       qt.setAuthorDisplayName(user.getDisplayName());
                       qt.setAuthorProfileImageUrl(user.getProfileImageUrl());
                       qt.setOriginalPostId(originalPostId);
                       qt.setQuoteTweetComment(comment);
                       qt.setHashtags(extractHashtags(comment));
                       qt.setMentions(extractMentions(comment));

                       return feedRepository.save(qt)
                                            .flatMap(savedQT -> {
                                                if (!originalPost.getRetweetedBy().contains(userId)) {
                                                    originalPost.getRetweetedBy().add(userId);
                                                    originalPost.setRetweetsCount(originalPost.getRetweetsCount() + 1);
                                                    originalPost.setUpdatedAt(Timestamp.now());
                                                    return calculateAndUpdateEngagementScore(originalPost)
                                                            .flatMap(feedRepository::save)
                                                            .thenReturn(savedQT);
                                                }
                                                return Mono.just(savedQT);
                                            });
                   });
    }

    public Flux<Feed> getUserFeed(String userId) {
        return feedRepository.findByAuthorIdAndParentIdIsNull(userId);
    }

    public Flux<Feed> getComments(String postId) {
        return feedRepository.findByParentId(postId);
    }

    public Mono<Feed> findById(String id) {
        return feedRepository.findById(id);
    }

    /**
     * Hard delete post; delete all comments; update parent if comment; publish event
     */
    public Mono<Feed> deletePost(String userId, String postId) {
        return feedRepository.findById(postId)
                             .switchIfEmpty(Mono.error(new RuntimeException("Post not found")))
                             .flatMap(post -> {
                                 if (!userId.equals(post.getAuthorId())) {
                                     return Mono.error(new RuntimeException("Unauthorized to delete this post"));
                                 }

                                 // Publish deletion event (non-fatal)
                                 try {
                                     feedEventPublisher.publishPostDeleted(postId, userId);
                                 } catch (Exception e) {
                                     log.error("Failed to publish post deleted event for {}: {}",
                                               postId,
                                               e.getMessage());
                                 }

                                 if (post.getParentId() != null) {
                                     // This is a comment - delete it and update parent counts
                                     return Mono.zip(
                                                        // Delete the comment
                                                        feedRepository.deleteById(postId),
                                                        // Update parent post counts
                                                        feedRepository.findById(post.getParentId())
                                                                      .flatMap(parent -> {
                                                                          parent.setCommentsCount(Math.max(0,
                                                                                                           parent.getCommentsCount() - 1));
                                                                          parent.setUpdatedAt(Timestamp.now());
                                                                          return calculateAndUpdateEngagementScore(parent)
                                                                                  .flatMap(feedRepository::save)
                                                                                  .doOnSuccess(updated -> {
                                                                                      try {
                                                                                          feedEventPublisher.publishPostUpdated(
                                                                                                  updated);
                                                                                      } catch (Exception e) {
                                                                                          log.error(
                                                                                                  "Failed to publish post updated event for {}: {}",
                                                                                                  updated.getId(),
                                                                                                  e.getMessage());
                                                                                      }
                                                                                  });
                                                                      })
                                                )
                                                .thenReturn(post);
                                 } else {
                                     // This is a main post - delete it and all its comments
                                     return feedRepository.findByParentId(postId)
                                                          .collectList()
                                                          .flatMap(comments -> {
                                                              // Delete all comments first
                                                              List<Mono<Void>> deleteOperations = comments.stream()
                                                                                                          .map(comment -> feedRepository.deleteById(
                                                                                                                  comment.getId()))
                                                                                                          .collect(
                                                                                                                  Collectors.toList());

                                                              return Flux.fromIterable(deleteOperations)
                                                                         .flatMap(operation -> operation)
                                                                         .then()
                                                                         .then(feedRepository.deleteById(postId))
                                                                         .thenReturn(post);
                                                          });
                                 }
                             });
    }

    /* ---------------------- Helpers ---------------------- */

    private User incrementPosts(User author) {
        author.setPostsCount(author.getPostsCount() + 1);
        author.setUpdatedAt(Timestamp.now());
        return author;
    }

    private List<String> extractHashtags(String content) {
        if (content == null) {
            return List.of();
        }
        Pattern pattern = Pattern.compile("#\\w+");
        Matcher matcher = pattern.matcher(content);
        return matcher.results().map(m -> m.group().substring(1)).toList();
    }

    private List<String> extractMentions(String content) {
        if (content == null) {
            return List.of();
        }
        Pattern pattern = Pattern.compile("@\\w+");
        Matcher matcher = pattern.matcher(content);
        return matcher.results().map(m -> m.group().substring(1)).toList();
    }

    /**
     * Calculate base engagement score and set it on the post (reactive).
     * score = likes*1.0 + retweets*1.5 + commentScore + timeDecay
     */
    private Mono<Feed> calculateAndUpdateEngagementScore(Feed post) {
        return calculateCommentScore(post)
                .map(commentScore -> {
                    double score = 0.0;
                    score += post.getLikesCount() * 1.0;
                    score += post.getRetweetsCount() * 1.5;
                    score += commentScore;
                    score += calculateTimeDecay(post.getCreatedAt());
                    post.setBaseEngagementScore(score);
                    log.debug(
                            "Updated engagement score for post {}: {} (likes: {}, retweets: {}, distinctCommenters: {})",
                            post.getId(),
                            score,
                            post.getLikesCount(),
                            post.getRetweetsCount(),
                            post.getDistinctCommentersCount());
                    return post;
                });
    }

    /**
     * Comment score with diminishing returns (reactive); updates distinctCommentersCount on post
     */
    private Mono<Double> calculateCommentScore(Feed post) {
        String key = (post.getId() != null) ? post.getId() : post.getRootId();
        if (key == null) {
            return Mono.just(0.0);
        }

        return feedRepository.findByParentId(key)
                             .collectList()
                             .map(comments -> {
                                 Map<String, Integer> perUser = new HashMap<>();
                                 for (Feed c : comments) {
                                     perUser.merge(c.getAuthorId(), 1, Integer::sum);
                                 }
                                 double commentScore = 0.0;
                                 Set<String> distinct = new HashSet<>();
                                 for (var e : perUser.entrySet()) {
                                     int n = e.getValue();
                                     distinct.add(e.getKey());
                                     if (n >= 1) {
                                         commentScore += 2.0;           // first
                                     }
                                     if (n >= 2) {
                                         commentScore += 1.0;           // second
                                     }
                                     if (n >= 3) {
                                         commentScore += (n - 2) * 0.5; // third+
                                     }
                                 }
                                 post.setDistinctCommentersCount(distinct.size());
                                 return commentScore;
                             });
    }

    /**
     * 0–10 time-decay boost
     */
    private double calculateTimeDecay(Timestamp createdAt) {
        if (createdAt == null) {
            return 0.0;
        }
        long hoursOld = ChronoUnit.HOURS.between(createdAt.toDate().toInstant(), Timestamp.now().toDate().toInstant());
        if (hoursOld <= 24) {
            return 10.0 - (hoursOld / 24.0) * 2.0;         // 8–10
        }
        if (hoursOld <= 168) {
            return 8.0 - ((hoursOld - 24) / 144.0) * 6.0; // 2–8
        }
        return Math.max(0, 2.0 - ((hoursOld - 168) / 168.0) * 2.0);        // 0–2
    }

    /**
     * Update distinct commenters count (reactive)
     */
    private Mono<Feed> updateDistinctCommentersCount(Feed post) {
        return feedRepository.findByParentId(post.getId())
                             .map(Feed::getAuthorId)
                             .distinct()
                             .count()
                             .map(cnt -> {
                                 post.setDistinctCommentersCount(cnt.intValue());
                                 return post;
                             });
    }
}
