package com.interviewai.domain;

import com.interviewai.common.enums.SubscriptionPlan;
import com.interviewai.common.enums.SubscriptionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "subscriptions")
public class Subscription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionPlan plan = SubscriptionPlan.FREE;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SubscriptionStatus status = SubscriptionStatus.TRIAL;

    @Column(name = "ai_quota_month", nullable = false)
    private int aiQuotaMonth = 50;

    @Column(name = "ai_used_month", nullable = false)
    private int aiUsedMonth;

    @Column(name = "starts_at", nullable = false, updatable = false)
    private Instant startsAt = Instant.now();

    @Column(name = "expires_at")
    private Instant expiresAt;
}
