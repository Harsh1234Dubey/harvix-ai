package com.interviewai.dto.request;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.InterviewType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.time.Instant;

public record ScheduleInterviewRequest(
        Long applicationId,
        @NotNull Long candidateId,
        String title,
        InterviewType type,
        @NotNull Instant scheduledAt,
        @Positive Integer durationMin,
        String location,
        String meetingLink,
        Difficulty difficulty
) {
}
