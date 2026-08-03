package com.interviewai.repository;

import com.interviewai.domain.CodingSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CodingSubmissionRepository extends JpaRepository<CodingSubmission, Long> {

    Optional<CodingSubmission> findByUuid(UUID uuid);

    Page<CodingSubmission> findByCandidateId(Long candidateId, Pageable pageable);

    List<CodingSubmission> findByCandidateIdAndCodingTestIdOrderBySubmittedAtDesc(Long candidateId, Long codingTestId);
}
