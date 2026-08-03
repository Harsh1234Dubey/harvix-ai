package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "ai_usage_logs")
public class AiUsageLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String feature;

    @Column(length = 100)
    private String model;

    @Column(name = "input_tokens")
    private int inputTokens;

    @Column(name = "output_tokens")
    private int outputTokens;

    @Column(name = "cost_estimate", precision = 10, scale = 6)
    private BigDecimal costEstimate = BigDecimal.ZERO;

    @Column(name = "latency_ms")
    private int latencyMs;

    @Column(name = "prompt_preview", columnDefinition = "text")
    private String promptPreview;

    @Column(nullable = false)
    private boolean success = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
