package com.interviewai.service;

import com.interviewai.ai.AiResumeService;
import com.interviewai.common.util.ResumeTextExtractor;
import com.interviewai.domain.AtsReport;
import com.interviewai.domain.Job;
import com.interviewai.domain.Resume;
import com.interviewai.domain.ResumeVersion;
import com.interviewai.domain.StoredFile;
import com.interviewai.domain.User;
import com.interviewai.dto.response.AtsReportResponse;
import com.interviewai.dto.response.AtsScoreResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.ResumeResponse;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.AtsReportRepository;
import com.interviewai.repository.ResumeRepository;
import com.interviewai.repository.ResumeVersionRepository;
import com.interviewai.storage.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeService {

    private final ResumeRepository resumeRepository;
    private final ResumeVersionRepository resumeVersionRepository;
    private final FileStorageService fileStorageService;
    private final AiResumeService aiResumeService;
    private final JobService jobService;
    private final AtsReportRepository atsReportRepository;

    @Transactional
    public ResumeResponse upload(User candidate, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new com.interviewai.exception.BadRequestException("A resume file is required");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".pdf")) {
            throw new com.interviewai.exception.BadRequestException("Only PDF resumes are supported");
        }
        StoredFile stored = fileStorageService.store(file, "resumes", candidate.getId(), "RESUME", null);
        Resume resume = resumeRepository.findByCandidateId(candidate.getId()).stream().findFirst()
                .orElseGet(() -> {
                    Resume created = new Resume();
                    created.setCandidate(candidate);
                    created.setTitle("My Resume");
                    return resumeRepository.save(created);
                });
        int nextVersion = resume.getCurrentVersion() + 1;
        resume.setCurrentVersion(nextVersion);
        resumeRepository.save(resume);

        ResumeVersion version = new ResumeVersion();
        version.setResume(resume);
        version.setVersionNo(nextVersion);
        version.setFilePath(stored.getStoragePath());
        version.setFileSize(stored.getSizeBytes());
        version.setFileType(stored.getMimeType());
        resumeVersionRepository.save(version);
        return ResumeResponse.from(resume, resumeVersionRepository.findByResumeIdOrderByVersionNoDesc(resume.getId()));
    }

    @Transactional(readOnly = true)
    public List<ResumeResponse> list(Long candidateId) {
        return resumeRepository.findByCandidateId(candidateId).stream()
                .map(r -> ResumeResponse.from(r, resumeVersionRepository.findByResumeIdOrderByVersionNoDesc(r.getId())))
                .toList();
    }

    @Transactional(readOnly = true)
    public Resume getByIdAndCandidate(Long id, Long candidateId) {
        return resumeRepository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> ResourceNotFoundException.of("Resume", id));
    }

    @Transactional(readOnly = true)
    public List<ResumeVersion> versions(Long resumeId) {
        return resumeVersionRepository.findByResumeIdOrderByVersionNoDesc(resumeId);
    }

    @Transactional
    public MessageResponse delete(Long id, Long candidateId) {
        Resume resume = getByIdAndCandidate(id, candidateId);
        resumeRepository.delete(resume);
        return MessageResponse.of("Resume deleted");
    }

    @Transactional
    public AtsScoreResponse atsScore(Long resumeId, Long candidateId, Long jobId) {
        Resume resume = getByIdAndCandidate(resumeId, candidateId);
        ResumeVersion version = latestVersion(resume.getId());
        Job job = jobService.getEntityById(jobId);
        String text = extractText(version);
        AiResumeService.AtsResult result = aiResumeService.analyze(text, job);

        AtsReport report = new AtsReport();
        report.setResumeId(resumeId);
        report.setCandidateId(candidateId);
        report.setVersionNo(version.getVersionNo());
        report.setJobId(jobId);
        report.setJobTitle(job.getTitle());
        report.setScore(result.score());
        report.setSummary(result.summary());
        report.setStrengths(result.strengths());
        report.setGaps(result.gaps());
        report.setMatchedKeywords(result.matchedKeywords());
        report.setMissingKeywords(result.missingKeywords());
        report.setSource(result.source());
        atsReportRepository.save(report);

        return AtsScoreResponse.of(resumeId, jobId, job.getTitle(), result, result.source());
    }

    @Transactional(readOnly = true)
    public List<AtsReportResponse> atsHistory(Long resumeId, Long candidateId) {
        getByIdAndCandidate(resumeId, candidateId);
        return atsReportRepository.findByResumeIdOrderByCreatedAtDesc(resumeId).stream()
                .map(AtsReportResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public BigDecimal computeAtsScore(Long resumeId, Long candidateId, Long jobId) {
        try {
            return atsScore(resumeId, candidateId, jobId).score();
        } catch (RuntimeException e) {
            log.warn("ATS scoring failed for resume {}: {}", resumeId, e.getMessage());
            return null;
        }
    }

    private ResumeVersion latestVersion(Long resumeId) {
        return resumeVersionRepository.findByResumeIdOrderByVersionNoDesc(resumeId).stream()
                .findFirst()
                .orElseThrow(() -> ResourceNotFoundException.of("ResumeVersion", resumeId));
    }

    private String extractText(ResumeVersion version) {
        try {
            return ResumeTextExtractor.extract(Path.of(version.getFilePath()));
        } catch (IOException | RuntimeException e) {
            log.warn("Could not extract resume text from {}: {}", version.getFilePath(), e.getMessage());
            return "";
        }
    }
}
