package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "leaderboard_entries", uniqueConstraints =
@UniqueConstraint(columnNames = {"user_id", "period"}))
public class LeaderboardEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 20)
    private String period = "ALL_TIME";

    @Column(name = "xp_total", nullable = false)
    private int xpTotal;

    @Column(name = "rank_no")
    private Integer rankNo;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();
}
