package io.shrouded.okara.mapper;

import io.shrouded.okara.dto.feed.FeedDto;
import io.shrouded.okara.model.Feed;
import io.shrouded.okara.model.FeedItem;
import io.shrouded.okara.util.TimestampUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {TimestampUtils.class})
public interface FeedMapper {

    @Mapping(target = "createdAt", expression = "java(TimestampUtils.toInstant(feed.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(TimestampUtils.toInstant(feed.getUpdatedAt()))")
    @Mapping(target = "isPinned", source = "pinned")
    FeedDto toFeedDto(Feed feed);

    @Mapping(target = "id", source = "postId")
    @Mapping(target = "type", source = "postType")
    @Mapping(target = "updatedAt", source = "createdAt")
    @Mapping(target = "viewsCount", constant = "0")
    @Mapping(target = "likedBy", expression = "java(new ArrayList<>())")
    @Mapping(target = "dislikedBy", expression = "java(new ArrayList<>())")
    @Mapping(target = "retweetedBy", expression = "java(new ArrayList<>())")
    @Mapping(target = "parentId", ignore = true)
    @Mapping(target = "rootId", ignore = true)
    @Mapping(target = "pinned", constant = "false")
    @Mapping(target = "hashtags", expression = "java(new ArrayList<>())")
    @Mapping(target = "mentions", expression = "java(new ArrayList<>())")
    @Mapping(target = "dislikesCount", constant = "0")
    @Mapping(target = "baseEngagementScore", source = "algorithmScore")
    @Mapping(target = "quoteTweetComment", source = "retweetComment")
    @Mapping(target = "imageUrls", expression = "java(feedItem.getImageUrls() != null ? feedItem.getImageUrls() : new ArrayList<>())")
    @Mapping(target = "distinctCommentersCount", constant = "0")
    @Mapping(target = "niches", expression = "java(new ArrayList<>())")
    Feed convertToFeed(FeedItem feedItem);
}