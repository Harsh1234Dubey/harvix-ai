package com.interviewai.repository;

import com.interviewai.domain.CodingStreak;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CodingStreakRepository extends JpaRepository<CodingStreak, Long> {
    Optional<CodingStreak> findByUserId(Long userId);
}
