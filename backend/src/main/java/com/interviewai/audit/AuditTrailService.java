package com.interviewai.audit;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.domain.AuditLog;
import com.interviewai.repository.AuditLogRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuditTrailService {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    @Async
    public void record(Long userId, com.interviewai.common.enums.AuditAction action,
                       String resource, String resourceId,
                       Object before, Object after) {
        try {
            AuditLog entry = new AuditLog();
            entry.setAction(action);
            entry.setResource(resource);
            entry.setResourceId(resourceId);
            entry.setEntityBefore(toJson(before));
            entry.setEntityAfter(toJson(after));
            HttpServletRequest request = currentRequest();
            if (request != null) {
                entry.setIpAddress(request.getRemoteAddr());
                entry.setUserAgent(request.getHeader("User-Agent"));
            }
            if (userId != null) {
                var ref = new com.interviewai.domain.User();
                ref.setId(userId);
                entry.setUser(ref);
            }
            auditLogRepository.save(entry);
        } catch (Exception ex) {
            log.warn("Failed to write audit log: {}", ex.getMessage());
        }
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            return String.valueOf(value);
        }
    }

    private HttpServletRequest currentRequest() {
        var attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }
}
