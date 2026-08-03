package com.interviewai.ai;

import com.interviewai.common.enums.SubmissionStatus;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record CodeReviewResult(
        SubmissionStatus status,
        int passed,
        String stdout,
        String stderr,
        String error,
        BigDecimal codeScore,
        String complexityTime,
        String complexitySpace,
        List<Map<String, Object>> caseResults
) {
}
