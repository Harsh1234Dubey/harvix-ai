package com.interviewai.repository;

import com.interviewai.domain.Subscription;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SubscriptionRepository extends JpaRepository<Subscription, Long> {
    Optional<Subscription> findByUserId(Long userId);
    Optional<Subscription> findByCompanyId(Long companyId);
    List<Subscription> findByStatus(String status);
}
