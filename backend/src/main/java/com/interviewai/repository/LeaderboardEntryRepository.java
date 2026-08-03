package com.interviewai.repository;

import com.interviewai.domain.LeaderboardEntry;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LeaderboardEntryRepository extends JpaRepository<LeaderboardEntry, Long> {

    Optional<LeaderboardEntry> findByUserIdAndPeriod(Long userId, String period);

    List<LeaderboardEntry> findByPeriodOrderByXpTotalDesc(String period, Pageable pageable);

    @Modifying
    @Query("update LeaderboardEntry l set l.xpTotal = :xp, l.updatedAt = current_timestamp where l.id = :id")
    void refreshXp(@Param("id") Long id, @Param("xp") int xp);
}
