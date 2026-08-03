package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.Company;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateJobRequest;
import com.interviewai.dto.response.JobResponse;
import com.interviewai.exception.AccessDeniedException;
import com.interviewai.repository.CompanyMemberRepository;
import com.interviewai.service.CompanyService;
import com.interviewai.service.JobService;
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
@RequestMapping(AppConstants.API_BASE_PATH + "/jobs")
@RequiredArgsConstructor
@Tag(name = "Jobs", description = "Job posting, publishing and search")
public class JobController {

    private final JobService jobService;
    private final CompanyService companyService;
    private final UserService userService;
    private final CompanyMemberRepository companyMemberRepository;

    @PostMapping
    @Operation(summary = "Create a job under a company (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> create(
            @RequestParam Long companyId, @Valid @RequestBody CreateJobRequest request, Principal principal) {
        User user = userService.currentUser(principal.getName());
        requireMember(companyId, user);
        Company company = companyService.findById(companyId);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Job created", jobService.create(request, company, user)));
    }

    @GetMapping
    @Operation(summary = "Search jobs with filters")
    public ResponseEntity<ApiResponse<PageResponse<JobResponse>>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) String location,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        return ResponseEntity.ok(ApiResponse.success(jobService.search(q, status, companyId, location, page, size, sort)));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get a job by public id")
    public ResponseEntity<ApiResponse<JobResponse>> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(jobService.get(uuid)));
    }

    @PatchMapping("/{uuid}/publish")
    @Operation(summary = "Publish a draft job (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> publish(@PathVariable UUID uuid, Principal principal) {
        requireJobCompanyMember(uuid, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Job published", jobService.publish(jobService.getEntity(uuid).getId())));
    }

    @PatchMapping("/{uuid}/close")
    @Operation(summary = "Close a job (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<JobResponse>> close(@PathVariable UUID uuid, Principal principal) {
        requireJobCompanyMember(uuid, principal.getName());
        return ResponseEntity.ok(ApiResponse.success("Job closed", jobService.close(jobService.getEntity(uuid).getId())));
    }

    @PostMapping("/{uuid}/view")
    @Operation(summary = "Increment job view counter")
    public ResponseEntity<ApiResponse<Void>> trackView(@PathVariable UUID uuid) {
        jobService.incrementViews(uuid);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    private void requireMember(Long companyId, User user) {
        boolean isAdmin = user.getRoles().stream().anyMatch(r -> r.getCode().equals("ADMIN"));
        boolean isMember = companyMemberRepository.existsByCompanyIdAndUserId(companyId, user.getId());
        if (!isAdmin && !isMember) {
            throw new AccessDeniedException("You are not a member of this company");
        }
    }

    private void requireJobCompanyMember(UUID jobUuid, String email) {
        User user = userService.currentUser(email);
        Long companyId = jobService.getEntity(jobUuid).getCompany().getId();
        requireMember(companyId, user);
    }
}
