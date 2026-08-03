package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.domain.StoredFile;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.UnauthorizedException;
import com.interviewai.repository.StoredFileRepository;
import com.interviewai.service.UserService;
import com.interviewai.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.security.Principal;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/files")
@RequiredArgsConstructor
@Tag(name = "Files", description = "Local file upload and download")
public class FileController {

    private final FileStorageService fileStorageService;
    private final StoredFileRepository storedFileRepository;
    private final UserService userService;

    @PostMapping("/upload")
    @Operation(summary = "Upload a file (authenticated)")
    public ResponseEntity<ApiResponse<Map<String, Object>>> upload(
            @RequestParam("file") MultipartFile file,
            @RequestParam(required = false) String category,
            Principal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Authentication required");
        }
        Long userId = userService.currentUser(principal.getName()).getId();
        StoredFile stored = fileStorageService.store(file, category != null ? category : "general", userId, null, null);
        return ResponseEntity.status(201).body(ApiResponse.created("File uploaded", Map.of(
                "uuid", stored.getUuid(),
                "originalName", stored.getOriginalName(),
                "sizeBytes", stored.getSizeBytes(),
                "mimeType", stored.getMimeType())));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Download a stored file by public id")
    public ResponseEntity<Resource> download(@PathVariable UUID uuid) {
        StoredFile stored = storedFileRepository.findByUuid(uuid)
                .orElseThrow(() -> com.interviewai.exception.ResourceNotFoundException.of("StoredFile", uuid));
        Resource resource = fileStorageService.load(uuid);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + stored.getOriginalName() + "\"")
                .contentType(org.springframework.http.MediaType.parseMediaType(
                        stored.getMimeType() != null ? stored.getMimeType() : "application/octet-stream"))
                .body(resource);
    }
}
