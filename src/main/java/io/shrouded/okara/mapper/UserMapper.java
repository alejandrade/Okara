package io.shrouded.okara.mapper;

import io.shrouded.okara.dto.user.UserDto;
import io.shrouded.okara.model.User;
import io.shrouded.okara.util.TimestampUtils;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring", imports = {TimestampUtils.class})
public interface UserMapper {

    @Mapping(target = "createdAt", expression = "java(TimestampUtils.toInstant(user.getCreatedAt()))")
    @Mapping(target = "updatedAt", expression = "java(TimestampUtils.toInstant(user.getUpdatedAt()))")
    @Mapping(target = "isPrivate", source = "private")
    UserDto toUserDto(User user);
}