package com.interviewai.dto.response;

public record DashboardStats(
        long totalApplications,
        long savedJobs,
        long interviewsScheduled,
        long interviewsCompleted,
        long submissions,
        int totalXp,
        int level,
        long notificationsUnread,
        Double averageInterviewScore,
        Double bestResumeScore
) {
}
