package com.interviewai.audit;

import com.interviewai.common.util.SecurityUtils;
import com.interviewai.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class AuditAspect {

    private final AuditTrailService auditTrailService;
    private final UserRepository userRepository;

    @Around("@annotation(audit)")
    public Object around(ProceedingJoinPoint joinPoint, AuditLog audit) throws Throwable {
        Object result = joinPoint.proceed();
        try {
            String email = SecurityUtils.currentUserEmail();
            Long userId = null;
            if (email != null) {
                userId = userRepository.findByEmail(email).map(u -> u.getId()).orElse(null);
            }
            String resourceId = extractResourceId(joinPoint.getArgs());
            auditTrailService.record(userId, audit.action(), audit.resource(), resourceId, null, result);
        } catch (Exception ex) {
            // audit failures must never break the business operation
        }
        return result;
    }

    private String extractResourceId(Object[] args) {
        for (Object arg : args) {
            if (arg instanceof Number number) {
                return String.valueOf(number.longValue());
            }
            if (arg instanceof String string && string.matches("\\d+")) {
                return string;
            }
        }
        return null;
    }
}
