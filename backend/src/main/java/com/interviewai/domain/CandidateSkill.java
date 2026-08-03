package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = "candidate_skills", uniqueConstraints =
@UniqueConstraint(columnNames = {"candidate_id", "skill_id"}))
public class CandidateSkill {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private Integer proficiency;

    @Column(precision = 4, scale = 1)
    private BigDecimal years;

    @Column(name = "last_used")
    private Integer lastUsed;

    @Column(nullable = false)
    private boolean endorsed = false;
}
