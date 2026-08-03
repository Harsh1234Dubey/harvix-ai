package com.interviewai.service;

import com.interviewai.common.enums.JobStatus;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Company;
import com.interviewai.domain.Job;
import com.interviewai.domain.JobSkill;
import com.interviewai.domain.Skill;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateJobRequest;
import com.interviewai.dto.response.JobResponse;
import com.interviewai.exception.BadRequestException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.JobRepository;
import com.interviewai.repository.JobSkillRepository;
import com.interviewai.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final CompanyService companyService;

    @Transactional
    public JobResponse create(CreateJobRequest request, Company company, User postedBy) {
        Job job = new Job();
        job.setCompany(company);
        job.setPostedBy(postedBy);
        job.setTitle(request.title());
        job.setSlug(CompanyService.slugify(request.title()) + "-" + System.currentTimeMillis() % 100000);
        job.setDescription(request.description());
        job.setRequirements(request.requirements());
        job.setResponsibilities(request.responsibilities());
        job.setLocation(request.location());
        if (request.workMode() != null) job.setWorkMode(request.workMode());
        if (request.employmentType() != null) job.setEmploymentType(request.employmentType());
        job.setExperienceMin(request.experienceMin());
        job.setExperienceMax(request.experienceMax());
        job.setSalaryMin(request.salaryMin());
        job.setSalaryMax(request.salaryMax());
        if (request.currency() != null) job.setCurrency(request.currency());
        if (request.vacancyCount() != null) job.setVacancyCount(request.vacancyCount());
        jobRepository.save(job);
        attachSkills(job, request.requiredSkills());
        return JobResponse.from(job, jobSkillRepository.findByJobId(job.getId()));
    }

    @Transactional
    public JobResponse publish(Long jobId) {
        Job job = findById(jobId);
        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(Instant.now());
        return JobResponse.from(jobRepository.save(job), jobSkillRepository.findByJobId(jobId));
    }

    @Transactional
    public JobResponse close(Long jobId) {
        Job job = findById(jobId);
        job.setStatus(JobStatus.CLOSED);
        return JobResponse.from(jobRepository.save(job), jobSkillRepository.findByJobId(jobId));
    }

    @Transactional(readOnly = true)
    public PageResponse<JobResponse> search(String q, String status, Long companyId, String location,
                                            int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Job> jobs;
        if (companyId != null) {
            jobs = jobRepository.findByCompanyId(companyId, pageable);
        } else if (status != null && !status.isBlank()) {
            jobs = jobRepository.findByStatus(JobStatus.valueOf(status.toUpperCase()), pageable);
        } else if (q != null && !q.isBlank()) {
            jobs = jobRepository.search(q, pageable);
        } else {
            jobs = jobRepository.findAll(pageable);
        }
        return PageResponse.from(jobs, jobs.stream()
                .map(job -> JobResponse.from(job, jobSkillRepository.findByJobId(job.getId())))
                .toList());
    }

    @Transactional(readOnly = true)
    public JobResponse get(UUID uuid) {
        Job job = jobRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("Job", uuid));
        return JobResponse.from(job, jobSkillRepository.findByJobId(job.getId()));
    }

    @Transactional
    public void incrementViews(UUID uuid) {
        jobRepository.findByUuid(uuid).ifPresent(job -> {
            job.setViewsCount(job.getViewsCount() + 1);
            jobRepository.save(job);
        });
    }

    @Transactional(readOnly = true)
    public Job findById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Job", id));
    }

    @Transactional(readOnly = true)
    public Job getEntity(UUID uuid) {
        return jobRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("Job", uuid));
    }

    @Transactional(readOnly = true)
    public Job getEntityById(Long id) {
        return jobRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Job", id));
    }

    @Transactional
    public void updateCounts(Job job) {
        jobRepository.save(job);
    }

    private void attachSkills(Job job, List<String> skillNames) {
        if (skillNames == null) {
            return;
        }
        for (String name : skillNames) {
            String normalized = name.trim();
            if (normalized.isEmpty()) {
                continue;
            }
            Skill skill = skillRepository.findByNameIgnoreCase(normalized)
                    .orElseGet(() -> {
                        Skill created = new Skill();
                        created.setName(normalized);
                        return skillRepository.save(created);
                    });
            JobSkill jobSkill = new JobSkill();
            jobSkill.setJob(job);
            jobSkill.setSkill(skill);
            jobSkillRepository.save(jobSkill);
        }
    }
}
