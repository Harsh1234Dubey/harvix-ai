package com.interviewai.dto.request;

import jakarta.validation.constraints.NotNull;

import java.time.Instant;

public record CreateInterviewSlotRequest(
        @NotNull Instant startsAt,
        @NotNull Instant endsAt
) {
}
