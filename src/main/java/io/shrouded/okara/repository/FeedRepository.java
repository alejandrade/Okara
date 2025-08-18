package io.shrouded.okara.repository;

import io.shrouded.okara.model.Feed;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FeedRepository extends MongoRepository<Feed, String> {
    
    // Get main feed posts (no parent) - chronological
    Page<Feed> findByParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(Pageable pageable);
    
    // Get main feed posts (no parent) - engagement algorithm
    Page<Feed> findByParentIdIsNullAndIsDeletedFalseOrderByBaseEngagementScoreDescCreatedAtDesc(Pageable pageable);
    
    // Get user's posts
    Page<Feed> findByAuthorIdAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(String authorId, Pageable pageable);
    
    // Get comments for a post
    List<Feed> findByParentIdAndIsDeletedFalseOrderByCreatedAtAsc(String parentId);
    
    // Get comments by root post (for threading)
    List<Feed> findByRootIdAndIsDeletedFalseOrderByCreatedAtAsc(String rootId);
    
    // Get user's timeline (posts from following)
    @Query("{ 'authorId': { '$in': ?0 }, 'parentId': null, 'isDeleted': false }")
    Page<Feed> findTimelineByFollowing(List<String> followingIds, Pageable pageable);
    
    // Search posts by content
    @Query("{ 'content': { '$regex': ?0, '$options': 'i' }, 'parentId': null, 'isDeleted': false }")
    Page<Feed> searchByContent(String searchTerm, Pageable pageable);
    
    // Get posts by hashtag
    Page<Feed> findByHashtagsContainingAndParentIdIsNullAndIsDeletedFalseOrderByCreatedAtDesc(String hashtag, Pageable pageable);
    
    // Count user's posts
    long countByAuthorIdAndParentIdIsNullAndIsDeletedFalse(String authorId);
}