package com.interviewai.repository;

import com.interviewai.domain.CodeReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodeReviewRepository extends JpaRepository<CodeReview, Long> {
    Optional<CodeReview> findBySubmissionId(Long submissionId);
}
