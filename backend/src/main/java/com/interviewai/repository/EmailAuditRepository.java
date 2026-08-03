package com.interviewai.repository;

import com.interviewai.domain.EmailAudit;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailAuditRepository extends JpaRepository<EmailAudit, Long> {
}
