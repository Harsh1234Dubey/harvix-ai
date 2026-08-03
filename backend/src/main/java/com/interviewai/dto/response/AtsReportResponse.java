package com.interviewai.dto.response;

import com.interviewai.domain.AtsReport;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record AtsReportResponse(
        Long id,
        Long resumeId,
        int versionNo,
        Long jobId,
        String jobTitle,
        BigDecimal score,
        String summary,
        List<String> strengths,
        List<String> gaps,
        List<String> matchedKeywords,
        List<String> missingKeywords,
        String source,
        Instant createdAt
) {
    public static AtsReportResponse from(AtsReport report) {
        return new AtsReportResponse(
                report.getId(),
                report.getResumeId(),
                report.getVersionNo(),
                report.getJobId(),
                report.getJobTitle(),
                report.getScore(),
                report.getSummary(),
                report.getStrengths(),
                report.getGaps(),
                report.getMatchedKeywords(),
                report.getMissingKeywords(),
                report.getSource(),
                report.getCreatedAt());
    }
}
