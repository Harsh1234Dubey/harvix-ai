package com.interviewai.dto.response;

import com.interviewai.domain.CodingSubmission;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record SubmissionResponse(
        Long id,
        UUID uuid,
        Long codingTestId,
        String language,
        String status,
        int passedCases,
        int totalCases,
        Long executionTimeMs,
        Long memoryUsedKb,
        String stdout,
        String stderr,
        String errorMessage,
        BigDecimal codeScore,
        String complexityTime,
        String complexitySpace,
        Instant submittedAt
) {
    public static SubmissionResponse from(CodingSubmission s) {
        return new SubmissionResponse(
                s.getId(),
                s.getUuid(),
                s.getCodingTest().getId(),
                s.getLanguage(),
                s.getStatus().name(),
                s.getPassedCases(),
                s.getTotalCases(),
                s.getExecutionTimeMs(),
                s.getMemoryUsedKb(),
                s.getStdout(),
                s.getStderr(),
                s.getErrorMessage(),
                s.getCodeScore(),
                s.getComplexityTime(),
                s.getComplexitySpace(),
                s.getSubmittedAt());
    }
}
