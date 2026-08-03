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
@Table(name = "interview_feedback")
public class InterviewFeedback {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "overall_score", precision = 5, scale = 2)
    private BigDecimal overallScore;

    @Column(precision = 5, scale = 2)
    private BigDecimal communication;

    @Column(precision = 5, scale = 2)
    private BigDecimal confidence;

    @Column(name = "technical_knowledge", precision = 5, scale = 2)
    private BigDecimal technicalKnowledge;

    @Column(precision = 5, scale = 2)
    private BigDecimal grammar;

    @Column(precision = 5, scale = 2)
    private BigDecimal fluency;

    @Column(name = "keyword_match", precision = 5, scale = 2)
    private BigDecimal keywordMatch;

    @Column(name = "speaking_speed", precision = 5, scale = 2)
    private BigDecimal speakingSpeed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "strengths_json", columnDefinition = "jsonb")
    private String strengthsJson;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "weaknesses_json", columnDefinition = "jsonb")
    private String weaknessesJson;

    @Column(name = "learning_suggestions", columnDefinition = "text")
    private String learningSuggestions;

    @Column(name = "hiring_recommendation", length = 30)
    private String hiringRecommendation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "detailed_json", columnDefinition = "jsonb")
    private String detailedJson;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
