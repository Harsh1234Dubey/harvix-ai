package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.AuditLog;
import com.interviewai.domain.Subscription;
import com.interviewai.dto.request.UpdateSubscriptionRequest;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.repository.AuditLogRepository;
import com.interviewai.repository.SubscriptionRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin", description = "Admin-only management endpoints")
public class AdminController {

    private final AuditLogRepository auditLogRepository;
    private final SubscriptionRepository subscriptionRepository;

    @GetMapping("/audit-logs")
    @Operation(summary = "List audit logs")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> auditLogs(
            @RequestParam(required = false) String resource,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<AuditLog> logs = resource != null && !resource.isBlank()
                ? auditLogRepository.findByResource(resource, pageable)
                : auditLogRepository.findAll(pageable);
        List<Map<String, Object>> content = logs.getContent().stream()
                .map(log -> Map.<String, Object>of(
                        "id", log.getId(),
                        "userId", log.getUser() != null ? log.getUser().getId() : null,
                        "action", log.getAction().name(),
                        "resource", log.getResource(),
                        "resourceId", log.getResourceId(),
                        "ip", log.getIpAddress(),
                        "createdAt", log.getCreatedAt().toString()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(PageResponse.from(logs, content)));
    }

    @GetMapping("/subscriptions")
    @Operation(summary = "List all subscriptions")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> subscriptions() {
        List<Map<String, Object>> subs = subscriptionRepository.findAll().stream()
                .map(s -> Map.<String, Object>of(
                        "id", s.getId(),
                        "companyId", s.getCompany() != null ? s.getCompany().getId() : null,
                        "userId", s.getUser() != null ? s.getUser().getId() : null,
                        "plan", s.getPlan().name(),
                        "status", s.getStatus().name(),
                        "aiQuota", s.getAiQuotaMonth(),
                        "aiUsed", s.getAiUsedMonth(),
                        "expiresAt", s.getExpiresAt() != null ? s.getExpiresAt().toString() : null))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(subs));
    }

    @PatchMapping("/subscriptions/{id}")
    @Operation(summary = "Update a subscription plan/quota")
    public ResponseEntity<ApiResponse<MessageResponse>> updateSubscription(
            @PathVariable Long id, @Valid @RequestBody UpdateSubscriptionRequest request) {
        Subscription subscription = subscriptionRepository.findById(id)
                .orElseThrow(() -> com.interviewai.exception.ResourceNotFoundException.of("Subscription", id));
        subscription.setPlan(request.plan());
        if (request.aiQuotaMonth() != null) {
            subscription.setAiQuotaMonth(request.aiQuotaMonth());
        }
        subscriptionRepository.save(subscription);
        return ResponseEntity.ok(ApiResponse.success("Subscription updated",
                MessageResponse.of("Subscription updated successfully")));
    }
}
