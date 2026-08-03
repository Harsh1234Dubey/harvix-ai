package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateCodingTestRequest;
import com.interviewai.dto.request.SubmitCodeRequest;
import com.interviewai.dto.response.CodingTestResponse;
import com.interviewai.dto.response.SubmissionResponse;
import com.interviewai.service.CodingService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/coding")
@RequiredArgsConstructor
@Tag(name = "Coding", description = "Coding tests, submissions and evaluation")
public class CodingController {

    private final CodingService codingService;
    private final UserService userService;

    @PostMapping("/tests")
    @Operation(summary = "Create a coding test (admin/recruiter)")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER')")
    public ResponseEntity<ApiResponse<CodingTestResponse>> createTest(
            @Valid @RequestBody CreateCodingTestRequest request, Principal principal) {
        User creator = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Coding test created", codingService.createTest(request, creator)));
    }

    @GetMapping("/tests")
    @Operation(summary = "List coding tests")
    public ResponseEntity<ApiResponse<PageResponse<CodingTestResponse>>> listTests(
            @RequestParam(required = false) String language,
            @RequestParam(required = false) String difficulty,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success(codingService.listTests(language, difficulty, page, size)));
    }

    @GetMapping("/tests/{id}")
    @Operation(summary = "Get a coding test by id")
    public ResponseEntity<ApiResponse<CodingTestResponse>> getTest(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(codingService.getTest(id)));
    }

    @GetMapping("/tests/{id}/cases")
    @Operation(summary = "Get public test cases for a coding test")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> publicCases(@PathVariable Long id) {
        List<Map<String, Object>> cases = codingService.publicCases(id).stream()
                .map(tc -> Map.<String, Object>of(
                        "orderIndex", tc.getOrderIndex(),
                        "inputData", tc.getInputData(),
                        "expectedOutput", tc.getExpectedOutput()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(cases));
    }

    @PostMapping("/submissions")
    @Operation(summary = "Submit code for evaluation (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<SubmissionResponse>> submit(
            @Valid @RequestBody SubmitCodeRequest request, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Submission evaluated", codingService.submit(request, candidate)));
    }

    @GetMapping("/submissions/me")
    @Operation(summary = "Submission history for the current candidate")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<List<SubmissionResponse>>> mySubmissions(
            @RequestParam(required = false) Long codingTestId, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(codingService.history(candidate.getId(), codingTestId)));
    }

    @GetMapping("/submissions/{uuid}")
    @Operation(summary = "Get a submission by public id")
    public ResponseEntity<ApiResponse<SubmissionResponse>> getSubmission(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(codingService.getSubmission(uuid)));
    }
}
