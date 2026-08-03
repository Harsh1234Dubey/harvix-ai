package com.interviewai.repository;

import com.interviewai.domain.InterviewSlot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;

public interface InterviewSlotRepository extends JpaRepository<InterviewSlot, Long> {
    List<InterviewSlot> findByRecruiterIdAndStartsAtBetween(Long recruiterId, Instant from, Instant to);
    boolean existsByRecruiterIdAndStartsAtAndEndsAt(Long recruiterId, Instant startsAt, Instant endsAt);
}
