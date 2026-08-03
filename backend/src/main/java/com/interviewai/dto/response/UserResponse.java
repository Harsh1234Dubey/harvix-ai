package com.interviewai.dto.response;

import com.interviewai.domain.Role;
import com.interviewai.domain.User;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UserResponse(
        Long id,
        UUID uuid,
        String firstName,
        String lastName,
        String email,
        String avatarUrl,
        String phone,
        String status,
        boolean emailVerified,
        List<String> roles,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getUuid(),
                user.getFirstName(),
                user.getLastName(),
                user.getEmail(),
                user.getAvatarUrl(),
                user.getPhone(),
                user.getStatus() != null ? user.getStatus().name() : null,
                user.isEmailVerified(),
                user.getRoles().stream().map(Role::getCode).toList(),
                user.getCreatedAt());
    }
}
