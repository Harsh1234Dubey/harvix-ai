package com.interviewai.repository;

import com.interviewai.domain.AtsReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AtsReportRepository extends JpaRepository<AtsReport, Long> {
    List<AtsReport> findByResumeIdOrderByCreatedAtDesc(Long resumeId);
    Optional<AtsReport> findFirstByCandidateIdOrderByScoreDesc(Long candidateId);
}
