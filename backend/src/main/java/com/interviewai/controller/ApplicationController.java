package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.request.ApplyJobRequest;
import com.interviewai.dto.request.UpdateApplicationStatusRequest;
import com.interviewai.dto.response.ApplicationResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.service.ApplicationService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.UUID;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/applications")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "Job applications, status tracking, withdrawal")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Apply to a job (candidate)")
    @PreAuthorize("hasAnyRole('CANDIDATE','ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> apply(
            @RequestParam UUID jobUuid, @Valid @RequestBody ApplyJobRequest request, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Application submitted", applicationService.apply(jobUuid, candidate, request)));
    }

    @GetMapping("/me")
    @Operation(summary = "List the candidate's own applications")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> myApplications(
            Principal principal,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(applicationService.listByCandidate(candidate.getId(), page, size, sort)));
    }

    @GetMapping("/job/{jobId}")
    @Operation(summary = "List applications for a job (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<ApplicationResponse>>> byJob(
            @PathVariable Long jobId,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        return ResponseEntity.ok(ApiResponse.success(applicationService.listByJob(jobId, page, size, sort)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update application status (shortlist/reject/offer)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateApplicationStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", applicationService.updateStatus(id, request)));
    }

    @PostMapping("/{id}/withdraw")
    @Operation(summary = "Withdraw an application (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ApplicationResponse>> withdraw(@PathVariable Long id, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(applicationService.withdraw(id, candidate.getId())));
    }
}
