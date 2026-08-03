package com.interviewai.dto.request;

import com.interviewai.common.enums.Difficulty;
import jakarta.validation.constraints.NotNull;

public record StartInterviewSessionRequest(
        Long interviewId,
        @NotNull String skill,
        @NotNull Difficulty difficulty
) {
}
