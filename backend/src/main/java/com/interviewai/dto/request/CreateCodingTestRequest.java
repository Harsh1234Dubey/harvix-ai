package com.interviewai.dto.request;

import com.interviewai.common.enums.Difficulty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record CreateCodingTestRequest(
        @NotBlank @Size(max = 255) String title,
        String description,
        @NotBlank String language,
        Difficulty difficulty,
        Integer timeLimitSec,
        Integer memoryLimitMb,
        String starterCode,
        List<String> hiddenTestCases
) {
}
