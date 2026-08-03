package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.response.AnalyticsSummary;
import com.interviewai.dto.response.RecruiterAnalytics;
import com.interviewai.service.AnalyticsService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/analytics")
@RequiredArgsConstructor
@Tag(name = "Analytics", description = "Platform and recruiter analytics")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final UserService userService;

    @GetMapping("/admin")
    @Operation(summary = "Platform-wide analytics summary (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<AnalyticsSummary>> platform(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.platformSummary()));
    }

    @GetMapping("/recruiter/{companyId}")
    @Operation(summary = "Recruiter analytics for a company")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<RecruiterAnalytics>> recruiter(@PathVariable Long companyId) {
        return ResponseEntity.ok(ApiResponse.success(analyticsService.recruiterAnalytics(companyId)));
    }
}
