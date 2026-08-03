package com.interviewai.dto.response;

import com.interviewai.domain.Job;
import com.interviewai.domain.JobSkill;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record JobResponse(
        Long id,
        UUID uuid,
        Long companyId,
        String companyName,
        String companyLogo,
        String title,
        String slug,
        String description,
        String location,
        String workMode,
        String employmentType,
        Integer experienceMin,
        Integer experienceMax,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        int vacancyCount,
        String status,
        int viewsCount,
        int applicationsCount,
        Instant expiresAt,
        Instant publishedAt,
        Instant createdAt,
        List<String> requiredSkills
) {
    public static JobResponse from(Job job, List<JobSkill> skills) {
        return new JobResponse(
                job.getId(),
                job.getUuid(),
                job.getCompany().getId(),
                job.getCompany().getName(),
                job.getCompany().getLogoUrl(),
                job.getTitle(),
                job.getSlug(),
                job.getDescription(),
                job.getLocation(),
                job.getWorkMode() != null ? job.getWorkMode().name() : null,
                job.getEmploymentType() != null ? job.getEmploymentType().name() : null,
                job.getExperienceMin(),
                job.getExperienceMax(),
                job.getSalaryMin(),
                job.getSalaryMax(),
                job.getCurrency(),
                job.getVacancyCount(),
                job.getStatus().name(),
                job.getViewsCount(),
                job.getApplicationsCount(),
                job.getExpiresAt(),
                job.getPublishedAt(),
                job.getCreatedAt(),
                skills.stream().map(js -> js.getSkill().getName()).toList());
    }
}
