package com.interviewai.dto.response;

import com.interviewai.ai.AiResumeService.AtsResult;

import java.math.BigDecimal;
import java.util.List;

public record AtsScoreResponse(
        Long resumeId,
        Long jobId,
        String jobTitle,
        BigDecimal score,
        String summary,
        List<String> strengths,
        List<String> gaps,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        String source
) {
    public static AtsScoreResponse of(Long resumeId, Long jobId, String jobTitle, AtsResult result, String source) {
        return new AtsScoreResponse(resumeId, jobId, jobTitle, result.score(), result.summary(),
                result.strengths(), result.gaps(), result.matchedKeywords(), result.missingKeywords(), source);
    }
}
