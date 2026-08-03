package com.interviewai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ApplyJobRequest(
        @Size(max = 5000) String coverLetter,
        Long resumeId
) {
}
