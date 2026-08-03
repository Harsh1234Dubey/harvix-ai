package com.interviewai.repository;

import com.interviewai.common.enums.InterviewStatus;
import com.interviewai.domain.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, Long> {

    Optional<Interview> findByUuid(UUID uuid);

    Page<Interview> findByCandidateId(Long candidateId, Pageable pageable);

    Page<Interview> findByRecruiterId(Long recruiterId, Pageable pageable);

    List<Interview> findByStatusAndScheduledAtBetween(InterviewStatus status, Instant from, Instant to);
}
