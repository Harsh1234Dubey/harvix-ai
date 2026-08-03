package com.interviewai.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "ats_reports")
public class AtsReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "resume_id", nullable = false)
    private Long resumeId;

    @Column(name = "candidate_id", nullable = false)
    private Long candidateId;

    @Column(name = "version_no", nullable = false)
    private int versionNo;

    @Column(name = "job_id", nullable = false)
    private Long jobId;

    @Column(name = "job_title", length = 200)
    private String jobTitle;

    @Column(name = "score", nullable = false, precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "summary", columnDefinition = "text")
    private String summary;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths", columnDefinition = "jsonb")
    private List<String> strengths = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "gaps", columnDefinition = "jsonb")
    private List<String> gaps = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "matched_keywords", columnDefinition = "jsonb")
    private List<String> matchedKeywords = new ArrayList<>();

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_keywords", columnDefinition = "jsonb")
    private List<String> missingKeywords = new ArrayList<>();

    @Column(name = "source", length = 20)
    private String source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
