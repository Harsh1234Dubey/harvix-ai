package com.interviewai.dto.response;

import com.interviewai.domain.Application;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ApplicationResponse(
        Long id,
        UUID uuid,
        Long jobId,
        String jobTitle,
        String companyName,
        Long candidateId,
        String candidateName,
        String status,
        String coverLetter,
        BigDecimal atsScore,
        BigDecimal matchPercentage,
        String recruiterNotes,
        Instant appliedAt,
        Instant updatedAt
) {
    public static ApplicationResponse from(Application a) {
        return new ApplicationResponse(
                a.getId(),
                a.getUuid(),
                a.getJob().getId(),
                a.getJob().getTitle(),
                a.getJob().getCompany().getName(),
                a.getCandidate().getId(),
                a.getCandidate().getFirstName() + " " + a.getCandidate().getLastName(),
                a.getStatus().name(),
                a.getCoverLetter(),
                a.getAtsScore(),
                a.getMatchPercentage(),
                a.getRecruiterNotes(),
                a.getAppliedAt(),
                a.getUpdatedAt());
    }
}
