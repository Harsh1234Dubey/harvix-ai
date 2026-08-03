package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "code_reviews")
public class CodeReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false, unique = true)
    private CodingSubmission submission;

    @Column(name = "time_complexity", length = 50)
    private String timeComplexity;

    @Column(name = "space_complexity", length = 50)
    private String spaceComplexity;

    @Column(name = "code_quality_score", precision = 5, scale = 2)
    private BigDecimal codeQualityScore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "naming_suggestions", columnDefinition = "jsonb")
    private String namingSuggestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "optimization_suggestions", columnDefinition = "jsonb")
    private String optimizationSuggestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "bugs_found", columnDefinition = "jsonb")
    private String bugsFound;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "best_practices", columnDefinition = "jsonb")
    private String bestPractices;

    @Column(name = "overall_review", columnDefinition = "text")
    private String overallReview;

    @Column(name = "reviewed_at", nullable = false, updatable = false)
    private Instant reviewedAt = Instant.now();
}
