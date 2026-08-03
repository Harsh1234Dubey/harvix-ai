package com.interviewai.dto.request;

import jakarta.validation.constraints.NotNull;

public record AtsScoreRequest(
        @NotNull Long jobId
) {
}
