package com.interviewai.dto.response;

import java.util.List;

public record AuthResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn,
        boolean rememberMe,
        UserResponse user
) {
}
