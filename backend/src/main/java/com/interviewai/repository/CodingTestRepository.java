package com.interviewai.repository;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.domain.CodingTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodingTestRepository extends JpaRepository<CodingTest, Long> {
    Page<CodingTest> findByLanguage(String language, Pageable pageable);
    Page<CodingTest> findByDifficulty(Difficulty difficulty, Pageable pageable);
    Optional<CodingTest> findByIdAndPublicTestTrue(Long id);
}
