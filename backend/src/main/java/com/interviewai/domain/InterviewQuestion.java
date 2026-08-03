package com.interviewai.domain;

import com.interviewai.common.enums.Difficulty;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "interview_questions")
public class InterviewQuestion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "session_id", nullable = false)
    private InterviewSession session;

    @Column(name = "question_text", nullable = false, columnDefinition = "text")
    private String questionText;

    @Column(length = 100)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Difficulty difficulty;

    @Column(length = 50)
    private String category;

    @Column(name = "is_follow_up", nullable = false)
    private boolean followUp = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "follow_up_of")
    private InterviewQuestion followUpOf;

    @Column(name = "order_index", nullable = false)
    private int orderIndex;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
