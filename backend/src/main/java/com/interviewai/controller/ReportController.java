package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.enums.ReportType;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.Report;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.ReportMetaResponse;
import com.interviewai.service.ReportService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/reports")
@RequiredArgsConstructor
@Tag(name = "Reports", description = "Generate and download PDF/CSV reports")
public class ReportController {

    private final ReportService reportService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "List reports")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','CANDIDATE')")
    public ResponseEntity<ApiResponse<PageResponse<ReportMetaResponse>>> list(
            @RequestParam(required = false) Long generatedBy,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        return ResponseEntity.ok(ApiResponse.success(reportService.list(generatedBy, page, size, sort)));
    }

    @PostMapping
    @Operation(summary = "Generate a report (resume/interview/coding/performance/hiring)")
    @PreAuthorize("hasAnyRole('ADMIN','RECRUITER','CANDIDATE')")
    public ResponseEntity<ApiResponse<ReportMetaResponse>> generate(
            @RequestBody Map<String, Object> request, Principal principal) {
        User user = userService.currentUser(principal.getName());
        ReportType type = ReportType.valueOf(String.valueOf(request.getOrDefault("type", "PERFORMANCE")).toUpperCase());
        String title = request.getOrDefault("title", type.name() + " Report").toString();
        String scope = request.get("scope") != null ? String.valueOf(request.get("scope")) : null;
        String data = request.get("data") != null ? String.valueOf(request.get("data")) : null;
        String format = request.getOrDefault("format", "PDF").toString().toUpperCase();
        Report report = reportService.generate(type, title, scope, data, user, null, format);
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Report generated", ReportMetaResponse.from(report)));
    }

    @GetMapping("/{uuid}/download")
    @Operation(summary = "Download a report as PDF or CSV")
    public ResponseEntity<Resource> download(@PathVariable UUID uuid, @RequestParam(defaultValue = "pdf") String format) {
        Report report = reportService.getByUuid(uuid);
        Resource resource = format.equalsIgnoreCase("csv")
                ? reportService.renderCsv(report) : reportService.renderPdf(report);
        String filename = report.getUuid() + "." + (format.equalsIgnoreCase("csv") ? "csv" : "pdf");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .contentType(format.equalsIgnoreCase("csv")
                        ? MediaType.parseMediaType("text/csv")
                        : MediaType.APPLICATION_PDF)
                .body(resource);
    }

    @DeleteMapping("/{uuid}")
    @Operation(summary = "Delete a report")
    public ResponseEntity<ApiResponse<MessageResponse>> delete(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(reportService.delete(uuid)));
    }
}
