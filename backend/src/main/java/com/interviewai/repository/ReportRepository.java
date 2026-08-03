package com.interviewai.repository;

import com.interviewai.common.enums.ReportType;
import com.interviewai.domain.Report;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByUuid(UUID uuid);
    Page<Report> findByGeneratedById(Long generatedById, Pageable pageable);
    Page<Report> findByReportType(ReportType reportType, Pageable pageable);
    Page<Report> findBySubjectUserId(Long subjectUserId, Pageable pageable);
}
