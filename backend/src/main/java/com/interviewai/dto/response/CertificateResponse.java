package com.interviewai.dto.response;

import com.interviewai.domain.Certificate;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record CertificateResponse(
        Long id,
        UUID uuid,
        String title,
        String description,
        String issuedFor,
        String grade,
        BigDecimal score,
        String filePath,
        Instant issuedAt
) {
    public static CertificateResponse from(Certificate c) {
        return new CertificateResponse(
                c.getId(),
                c.getUuid(),
                c.getTitle(),
                c.getDescription(),
                c.getIssuedFor(),
                c.getGrade(),
                c.getScore(),
                c.getFilePath(),
                c.getIssuedAt());
    }
}
