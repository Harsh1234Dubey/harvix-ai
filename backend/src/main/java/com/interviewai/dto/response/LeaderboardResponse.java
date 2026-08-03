package com.interviewai.dto.response;

public record LeaderboardResponse(
        String rank,
        Long userId,
        String name,
        String email,
        String avatarUrl,
        int xp
) {
}
