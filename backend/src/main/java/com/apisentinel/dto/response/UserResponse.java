package com.apisentinel.dto.response;

import com.apisentinel.entity.User;
import com.apisentinel.enums.Role;

import java.time.Instant;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

    public static UserResponse from(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getRole(), u.getCreatedAt());
    }
}
