package com.interviewai.repository;

import com.interviewai.domain.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface XpTransactionRepository extends JpaRepository<XpTransaction, Long> {

    List<XpTransaction> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("select coalesce(sum(x.xpChange), 0) from XpTransaction x where x.user.id = :userId")
    int sumXp(@Param("userId") Long userId);
}
