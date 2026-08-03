package com.interviewai.notification;

import com.interviewai.domain.EmailAudit;
import com.interviewai.repository.EmailAuditRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailSimulator {

    private final EmailAuditRepository emailAuditRepository;

    @Async
    public void send(String toEmail, String subject, String body) {
        log.info("[SIMULATED EMAIL] To: {} | Subject: {}", toEmail, subject);
        EmailAudit audit = new EmailAudit();
        audit.setToEmail(toEmail);
        audit.setSubject(subject);
        audit.setBody(body);
        emailAuditRepository.save(audit);
    }
}
