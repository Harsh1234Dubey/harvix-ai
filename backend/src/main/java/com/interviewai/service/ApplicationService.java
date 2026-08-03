package com.interviewai.service;

import com.interviewai.common.enums.ApplicationStatus;
import com.interviewai.common.enums.NotificationType;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Application;
import com.interviewai.domain.Job;
import com.interviewai.domain.User;
import com.interviewai.dto.request.ApplyJobRequest;
import com.interviewai.dto.request.UpdateApplicationStatusRequest;
import com.interviewai.dto.response.ApplicationResponse;
import com.interviewai.exception.DuplicateResourceException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.notification.NotificationService;
import com.interviewai.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final JobService jobService;
    private final NotificationService notificationService;
    private final ResumeService resumeService;

    @Transactional
    public ApplicationResponse apply(UUID jobUuid, User candidate, ApplyJobRequest request) {
        Job job = jobService.getEntity(jobUuid);
        applicationRepository.findByJobIdAndCandidateId(job.getId(), candidate.getId()).ifPresent(existing -> {
            throw new DuplicateResourceException("You have already applied for this job");
        });
        Application application = new Application();
        application.setJob(job);
        application.setCandidate(candidate);
        application.setCoverLetter(request.coverLetter());
        application.setStatus(ApplicationStatus.SUBMITTED);
        if (request.resumeId() != null) {
            application.setResume(new com.interviewai.domain.Resume());
            application.getResume().setId(request.resumeId());
            application.setAtsScore(resumeService.computeAtsScore(request.resumeId(), candidate.getId(), job.getId()));
        }
        Application saved = applicationRepository.save(application);
        job.setApplicationsCount(job.getApplicationsCount() + 1);
        jobService.updateCounts(job);

        notificationService.send(job.getPostedBy().getId(), NotificationType.APPLICATION_STATUS,
                "New application",
                candidate.getFirstName() + " applied for " + job.getTitle(),
                "{\"applicationId\":" + saved.getId() + "}");
        return ApplicationResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listByCandidate(Long candidateId, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Application> applications = applicationRepository.findByCandidateId(candidateId, pageable);
        return PageResponse.from(applications, applications.stream().map(ApplicationResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<ApplicationResponse> listByJob(Long jobId, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Application> applications = applicationRepository.findByJobId(jobId, pageable);
        return PageResponse.from(applications, applications.stream().map(ApplicationResponse::from).toList());
    }

    @Transactional
    public ApplicationResponse updateStatus(Long applicationId, UpdateApplicationStatusRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        application.setStatus(request.status());
        application.setRecruiterNotes(request.recruiterNotes());
        Application saved = applicationRepository.save(application);

        notificationService.send(application.getCandidate().getId(), NotificationType.APPLICATION_STATUS,
                "Application " + request.status().name().toLowerCase(),
                "Your application for '" + application.getJob().getTitle() + "' is now " + request.status().name().toLowerCase(),
                "{\"applicationId\":" + saved.getId() + "}");
        return ApplicationResponse.from(saved);
    }

    @Transactional
    public ApplicationResponse withdraw(Long applicationId, Long candidateId) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Application", applicationId));
        if (!application.getCandidate().getId().equals(candidateId)) {
            throw new com.interviewai.exception.AccessDeniedException("Cannot withdraw another user's application");
        }
        application.setStatus(ApplicationStatus.WITHDRAWN);
        return ApplicationResponse.from(applicationRepository.save(application));
    }
}
