package com.interviewai.domain;

import com.interviewai.common.enums.AuditAction;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_logs")
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private AuditAction action;

    @Column(length = 100)
    private String resource;

    @Column(name = "resource_id", length = 50)
    private String resourceId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_before", columnDefinition = "jsonb")
    private String entityBefore;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "entity_after", columnDefinition = "jsonb")
    private String entityAfter;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "user_agent", length = 255)
    private String userAgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
