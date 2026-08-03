package com.interviewai.repository;

import com.interviewai.domain.AiUsageLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface AiUsageLogRepository extends JpaRepository<AiUsageLog, Long> {

    @Query("select coalesce(sum(a.inputTokens + a.outputTokens), 0) from AiUsageLog a " +
            "where a.user.id = :userId and a.createdAt >= :from")
    long totalTokensSince(@Param("userId") Long userId, @Param("from") Instant from);

    @Query("select a.feature, count(a) from AiUsageLog a group by a.feature order by count(a) desc")
    List<Object[]> countByFeature();
}
