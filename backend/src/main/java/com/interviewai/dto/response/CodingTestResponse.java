package com.interviewai.dto.response;

import com.interviewai.domain.CodingTest;

import java.time.Instant;

public record CodingTestResponse(
        Long id,
        String title,
        String description,
        String language,
        String difficulty,
        int timeLimitSec,
        int memoryLimitMb,
        String starterCode,
        boolean publicTest,
        Instant createdAt
) {
    public static CodingTestResponse from(CodingTest t) {
        return new CodingTestResponse(
                t.getId(),
                t.getTitle(),
                t.getDescription(),
                t.getLanguage(),
                t.getDifficulty().name(),
                t.getTimeLimitSec(),
                t.getMemoryLimitMb(),
                t.getStarterCode(),
                t.isPublicTest(),
                t.getCreatedAt());
    }
}
