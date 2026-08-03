package com.interviewai.service;

import com.interviewai.common.enums.ApplicationStatus;
import com.interviewai.common.enums.JobStatus;
import com.interviewai.dto.response.AnalyticsSummary;
import com.interviewai.dto.response.RecruiterAnalytics;
import com.interviewai.repository.ApplicationRepository;
import com.interviewai.repository.CompanyRepository;
import com.interviewai.repository.InterviewRepository;
import com.interviewai.repository.JobRepository;
import com.interviewai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final InterviewRepository interviewRepository;

    @Transactional(readOnly = true)
    public AnalyticsSummary platformSummary() {
        Map<String, Long> applicationsByStatus = new LinkedHashMap<>();
        for (ApplicationStatus status : ApplicationStatus.values()) {
            applicationsByStatus.put(status.name(), applicationRepository.countByStatus(status));
        }
        Map<String, Long> jobsByStatus = new LinkedHashMap<>();
        for (JobStatus status : JobStatus.values()) {
            jobsByStatus.put(status.name(), jobRepository.countByStatus(status));
        }
        long recruiters = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode().equals("RECRUITER")))
                .count();
        long candidates = userRepository.findAll().stream()
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getCode().equals("CANDIDATE")))
                .count();
        long pendingVerifications = userRepository.findAll().stream()
                .filter(u -> !u.isEmailVerified()).count();
        return new AnalyticsSummary(
                userRepository.count(),
                recruiters,
                candidates,
                companyRepository.count(),
                jobRepository.count(),
                applicationRepository.count(),
                interviewRepository.count(),
                pendingVerifications,
                applicationsByStatus,
                jobsByStatus);
    }

    @Transactional(readOnly = true)
    public RecruiterAnalytics recruiterAnalytics(Long companyId) {
        List<com.interviewai.domain.Application> applications = applicationRepository.findAll().stream()
                .filter(a -> a.getJob().getCompany().getId().equals(companyId))
                .toList();
        long submitted = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.SUBMITTED).count();
        long reviewing = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.REVIEWING).count();
        long shortlisted = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.SHORTLISTED).count();
        long interviewed = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.INTERVIEW).count();
        long hired = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.HIRED).count();
        long rejected = applications.stream().filter(a -> a.getStatus() == ApplicationStatus.REJECTED).count();

        Map<String, Long> applicationsByJob = new LinkedHashMap<>();
        applications.forEach(a -> applicationsByJob.merge(a.getJob().getTitle(), 1L, Long::sum));

        return new RecruiterAnalytics(
                applications.size(),
                submitted,
                reviewing,
                shortlisted,
                interviewed,
                hired,
                rejected,
                applicationsByJob);
    }
}
