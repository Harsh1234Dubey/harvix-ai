package com.interviewai.dto.request;

import com.interviewai.common.enums.EmploymentType;
import com.interviewai.common.enums.WorkMode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateJobRequest(
        @NotBlank(message = "Job title is required")
        @Size(max = 255)
        String title,

        @NotBlank(message = "Job description is required")
        String description,

        String requirements,
        String responsibilities,
        String location,
        WorkMode workMode,
        EmploymentType employmentType,
        Integer experienceMin,
        Integer experienceMax,
        BigDecimal salaryMin,
        BigDecimal salaryMax,
        String currency,
        Integer vacancyCount,
        List<String> requiredSkills
) {
}
