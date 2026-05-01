package com.notfound.userservice.model.mapper;

import com.notfound.userservice.model.dto.response.UserResponse;
import com.notfound.userservice.model.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "id", target = "id")
    @Mapping(source = "username", target = "username")
    @Mapping(source = "email", target = "email")
    @Mapping(source = "fullName", target = "fullName")
    @Mapping(source = "phoneNumber", target = "phoneNumber")
    @Mapping(source = "role", target = "role")
    @Mapping(source = "gender", target = "gender")
    @Mapping(source = "status", target = "status")
    @Mapping(source = "dateOfBirth", target = "dateOfBirth")
    @Mapping(source = "points", target = "points")
    @Mapping(source = "membershipTier", target = "membershipTier")
    @Mapping(source = "isEmailVerified", target = "isEmailVerified")
    @Mapping(source = "authProvider", target = "authProvider")
    @Mapping(source = "lastLogin", target = "lastLogin")
    @Mapping(source = "avatar_url", target = "avatar")
    UserResponse toUserResponse(User user);
}
