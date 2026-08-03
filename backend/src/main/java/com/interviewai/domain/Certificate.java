package com.interviewai.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "certificates")
public class Certificate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, updatable = false)
    private UUID uuid;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_id", nullable = false)
    private User candidate;

    @Column(nullable = false, length = 255)
    private String title;

    @Column(columnDefinition = "text")
    private String description;

    @Column(name = "issued_for", length = 50)
    private String issuedFor;

    @Column(name = "reference_id")
    private Long referenceId;

    @Column(length = 30)
    private String grade;

    @Column(precision = 5, scale = 2)
    private BigDecimal score;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "issued_at", nullable = false, updatable = false)
    private Instant issuedAt = Instant.now();

    @PrePersist
    void onCreate() {
        this.uuid = UUID.randomUUID();
    }
}
