package com.interviewai.dto.response;

import com.interviewai.domain.Report;

import java.time.Instant;
import java.util.UUID;

public record ReportMetaResponse(
        UUID uuid,
        String reportType,
        String title,
        Long generatedBy,
        String format,
        Instant createdAt
) {
    public static ReportMetaResponse from(Report r) {
        return new ReportMetaResponse(
                r.getUuid(),
                r.getReportType().name(),
                r.getTitle(),
                r.getGeneratedBy() != null ? r.getGeneratedBy().getId() : null,
                r.getFormat(),
                r.getCreatedAt());
    }
}
