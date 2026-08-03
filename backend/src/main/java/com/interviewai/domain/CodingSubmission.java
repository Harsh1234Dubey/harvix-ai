package com.interviewai.domain;

import com.interviewai.common.enums.SubmissionStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "coding_submissions")
public class CodingSubmission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "coding_test_id", nullable = false)
    private CodingTest codingTest;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private InterviewSession session;

    @Column(nullable = false, length = 30)
    private String language;

    @Column(name = "source_code", nullable = false, columnDefinition = "text")
    private String sourceCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionStatus status = SubmissionStatus.PENDING;

    @Column(name = "passed_cases")
    private int passedCases;

    @Column(name = "total_cases")
    private int totalCases;

    @Column(name = "execution_time_ms")
    private Long executionTimeMs;

    @Column(name = "memory_used_kb")
    private Long memoryUsedKb;

    @Column(columnDefinition = "text")
    private String stdout;

    @Column(columnDefinition = "text")
    private String stderr;

    @Column(name = "error_message", columnDefinition = "text")
    private String errorMessage;

    @Column(name = "code_score", precision = 5, scale = 2)
    private BigDecimal codeScore;

    @Column(name = "complexity_time", length = 50)
    private String complexityTime;

    @Column(name = "complexity_space", length = 50)
    private String complexitySpace;

    @Column(name = "submitted_at", nullable = false, updatable = false)
    private Instant submittedAt;

    @PrePersist
    void onCreate() {
        this.uuid = UUID.randomUUID();
        this.submittedAt = Instant.now();
    }
}
