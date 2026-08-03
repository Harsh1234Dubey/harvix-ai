package com.interviewai.dto.response;

import java.util.Map;

public record AnalyticsSummary(
        long totalUsers,
        long totalRecruiters,
        long totalCandidates,
        long totalCompanies,
        long totalJobs,
        long totalApplications,
        long totalInterviews,
        long pendingVerifications,
        Map<String, Long> applicationsByStatus,
        Map<String, Long> jobsByStatus
) {
}
