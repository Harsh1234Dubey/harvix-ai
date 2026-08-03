package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "interview_answers")
public class InterviewAnswer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "question_id", nullable = false)
    private InterviewQuestion question;

    @Column(name = "answer_text", columnDefinition = "text")
    private String answerText;

    @Column(name = "audio_path", length = 500)
    private String audioPath;

    @Column(name = "voice_to_text", columnDefinition = "text")
    private String voiceToText;

    @Column(name = "confidence_score", precision = 5, scale = 2)
    private BigDecimal confidenceScore;

    @Column(name = "is_skipped", nullable = false)
    private boolean skipped = false;

    @Column(name = "answered_at", nullable = false, updatable = false)
    private Instant answeredAt = Instant.now();
}
