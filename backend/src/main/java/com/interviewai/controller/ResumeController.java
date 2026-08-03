package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.request.AtsScoreRequest;
import com.interviewai.dto.response.AtsReportResponse;
import com.interviewai.dto.response.AtsScoreResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.ResumeResponse;
import com.interviewai.service.ResumeService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/resumes")
@RequiredArgsConstructor
@Tag(name = "Resumes", description = "Resume upload and version history")
public class ResumeController {

    private final ResumeService resumeService;
    private final UserService userService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a resume PDF (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<ResumeResponse>> upload(
            @RequestParam("file") MultipartFile file, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Resume uploaded", resumeService.upload(candidate, file)));
    }

    @GetMapping("/me")
    @Operation(summary = "List the candidate's resumes with version history")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<ResumeResponse>>> list(Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(resumeService.list(candidate.getId())));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a resume (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(@PathVariable Long id, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(resumeService.delete(id, candidate.getId())));
    }

    @PostMapping("/{id}/ats-score")
    @Operation(summary = "Compute ATS score of a resume against a job (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<AtsScoreResponse>> atsScore(
            @PathVariable Long id, @Valid @RequestBody AtsScoreRequest request, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(resumeService.atsScore(id, candidate.getId(), request.jobId())));
    }

    @GetMapping("/{id}/ats-history")
    @Operation(summary = "ATS score history for a resume (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<AtsReportResponse>>> atsHistory(
            @PathVariable Long id, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(resumeService.atsHistory(id, candidate.getId())));
    }
}
