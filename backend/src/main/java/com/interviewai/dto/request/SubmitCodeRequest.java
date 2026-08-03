package com.interviewai.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitCodeRequest(
        @NotNull Long codingTestId,
        @NotBlank String language,
        @NotBlank String sourceCode
) {
}
