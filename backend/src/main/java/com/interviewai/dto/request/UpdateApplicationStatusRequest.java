package com.interviewai.dto.request;

import com.interviewai.common.enums.ApplicationStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateApplicationStatusRequest(
        @NotNull(message = "Status is required")
        ApplicationStatus status,
        String recruiterNotes
) {
}
