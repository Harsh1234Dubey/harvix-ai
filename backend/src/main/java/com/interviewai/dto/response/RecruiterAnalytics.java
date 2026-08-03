package com.interviewai.dto.response;

import java.util.Map;

public record RecruiterAnalytics(
        long totalApplications,
        long submitted,
        long reviewing,
        long shortlisted,
        long interviewed,
        long hired,
        long rejected,
        Map<String, Long> applicationsByJob
) {
}
