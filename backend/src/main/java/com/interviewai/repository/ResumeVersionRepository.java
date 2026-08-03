package com.interviewai.repository;

import com.interviewai.domain.ResumeVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ResumeVersionRepository extends JpaRepository<ResumeVersion, Long> {
    List<ResumeVersion> findByResumeIdOrderByVersionNoDesc(Long resumeId);
    Optional<ResumeVersion> findByResumeIdAndVersionNo(Long resumeId, int versionNo);
    long countByResumeId(Long resumeId);
}
