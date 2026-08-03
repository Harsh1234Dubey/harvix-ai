package com.interviewai.repository;

import com.interviewai.domain.TestCase;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TestCaseRepository extends JpaRepository<TestCase, Long> {
    List<TestCase> findByCodingTestIdOrderByOrderIndexAsc(Long codingTestId);
    long countByCodingTestId(Long codingTestId);
}
