package com.interviewai.dto.response;

import com.interviewai.domain.Interview;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record InterviewResponse(
        Long id,
        UUID uuid,
        Long applicationId,
        Long candidateId,
        String candidateName,
        Long recruiterId,
        String title,
        String type,
        String status,
        Instant scheduledAt,
        int durationMin,
        String location,
        String meetingLink,
        String difficulty,
        BigDecimal score,
        String hiringRecommendation,
        String feedbackSummary
) {
    public static InterviewResponse from(Interview i) {
        return new InterviewResponse(
                i.getId(),
                i.getUuid(),
                i.getApplication() != null ? i.getApplication().getId() : null,
                i.getCandidate().getId(),
                i.getCandidate().getFirstName() + " " + i.getCandidate().getLastName(),
                i.getRecruiter() != null ? i.getRecruiter().getId() : null,
                i.getTitle(),
                i.getType().name(),
                i.getStatus().name(),
                i.getScheduledAt(),
                i.getDurationMin(),
                i.getLocation(),
                i.getMeetingLink(),
                i.getDifficulty() != null ? i.getDifficulty().name() : null,
                i.getScore(),
                i.getHiringRecommendation(),
                i.getFeedbackSummary());
    }
}
