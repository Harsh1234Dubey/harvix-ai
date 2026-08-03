package com.interviewai.repository;

import com.interviewai.domain.InterviewSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewSessionRepository extends JpaRepository<InterviewSession, Long> {

    Optional<InterviewSession> findByUuid(UUID uuid);

    Page<InterviewSession> findByCandidateId(Long candidateId, Pageable pageable);

    List<InterviewSession> findByCandidateId(Long candidateId);

    Optional<InterviewSession> findTopByCandidateIdOrderByIdDesc(Long candidateId);
}
