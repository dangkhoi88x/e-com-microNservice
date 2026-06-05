package com.example.microserviceecom.mapper;

import com.example.microserviceecom.dto.request.CreateUserRequest;
import com.example.microserviceecom.dto.response.CreateUserResponse;
import com.example.microserviceecom.dto.response.UserResponse;
import com.example.microserviceecom.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper
public interface UserMapper {

    UserMapper INSTANCE = Mappers.getMapper(UserMapper.class );

    @Mapping(target = "email", source = "email")
    @Mapping(target = "password", ignore = true)
    User toUser(CreateUserRequest request);

    CreateUserResponse toCreateUserResponse(User user);


}
