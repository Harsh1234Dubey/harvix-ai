package com.interviewai.domain;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.InterviewStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "interview_sessions")
public class InterviewSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "interview_id")
    private Interview interview;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(length = 100)
    private String skill;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Difficulty difficulty = Difficulty.MEDIUM;

    @Column(name = "total_questions")
    private int totalQuestions;

    @Column(name = "answered_questions")
    private int answeredQuestions;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "transcript_json", columnDefinition = "jsonb")
    private String transcriptJson;

    @Column(name = "video_path", length = 500)
    private String videoPath;

    @Column(name = "duration_seconds")
    private int durationSeconds;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private InterviewStatus status = InterviewStatus.IN_PROGRESS;

    @PrePersist
    void onCreate() {
        this.uuid = UUID.randomUUID();
    }
}
