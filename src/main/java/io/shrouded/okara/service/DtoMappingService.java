package io.shrouded.okara.service;

import io.shrouded.okara.dto.FeedDto;
import io.shrouded.okara.dto.ProblemDetail;
import io.shrouded.okara.dto.UserDto;
import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.User;
import io.shrouded.okara.util.TimestampUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class DtoMappingService {

    public UserDto toUserDto(User user) {
        if (user == null) return null;
        
        return UserDto.builder()
            .id(user.getId())
            .username(user.getUsername())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .bio(user.getBio())
            .profileImageUrl(user.getProfileImageUrl())
            .bannerImageUrl(user.getBannerImageUrl())
            .location(user.getLocation())
            .website(user.getWebsite())
            .createdAt(TimestampUtils.toInstant(user.getCreatedAt()))
            .updatedAt(TimestampUtils.toInstant(user.getUpdatedAt()))
            .following(user.getFollowing())
            .followers(user.getFollowers())
            .followingCount(user.getFollowingCount())
            .followersCount(user.getFollowersCount())
            .postsCount(user.getPostsCount())
            .verified(user.isVerified())
            .isPrivate(user.isPrivate())
            .build();
    }

    public FeedDto toFeedDto(Feed feed) {
        if (feed == null) return null;
        
        return FeedDto.builder()
            .id(feed.getId())
            .authorId(feed.getAuthorId())
            .authorUsername(feed.getAuthorUsername())
            .authorDisplayName(feed.getAuthorDisplayName())
            .authorProfileImageUrl(feed.getAuthorProfileImageUrl())
            .content(feed.getContent())
            .imageUrls(feed.getImageUrls())
            .videoUrl(feed.getVideoUrl())
            .type(feed.getType())
            .parentId(feed.getParentId())
            .rootId(feed.getRootId())
            .createdAt(TimestampUtils.toInstant(feed.getCreatedAt()))
            .updatedAt(TimestampUtils.toInstant(feed.getUpdatedAt()))
            .likedBy(feed.getLikedBy())
            .dislikedBy(feed.getDislikedBy())
            .retweetedBy(feed.getRetweetedBy())
            .likesCount(feed.getLikesCount())
            .dislikesCount(feed.getDislikesCount())
            .retweetsCount(feed.getRetweetsCount())
            .commentsCount(feed.getCommentsCount())
            .viewsCount(feed.getViewsCount())
            .baseEngagementScore(feed.getBaseEngagementScore())
            .distinctCommentersCount(feed.getDistinctCommentersCount())
            .niches(feed.getNiches())
            .originalPostId(feed.getOriginalPostId())
            .quoteTweetComment(feed.getQuoteTweetComment())
            .isPinned(feed.isPinned())
            .hashtags(feed.getHashtags())
            .mentions(feed.getMentions())
            .build();
    }
}