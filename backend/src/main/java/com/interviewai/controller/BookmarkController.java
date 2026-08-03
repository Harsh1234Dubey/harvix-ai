package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.service.BookmarkService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/bookmarks")
@RequiredArgsConstructor
@Tag(name = "Bookmarks", description = "Save jobs and questions")
public class BookmarkController {

    private final BookmarkService bookmarkService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Bookmark an entity (JOB/QUESTION)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MessageResponse>> add(
            @RequestParam String entityType, @RequestParam Long entityId, Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.status(201).body(ApiResponse.created("Bookmarked", bookmarkService.add(user, entityType, entityId)));
    }

    @DeleteMapping
    @Operation(summary = "Remove a bookmark")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MessageResponse>> remove(
            @RequestParam String entityType, @RequestParam Long entityId, Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.remove(user, entityType, entityId)));
    }

    @GetMapping("/me")
    @Operation(summary = "List the current user's bookmarks")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<PageResponse<Map<String, Object>>>> list(
            @RequestParam String entityType,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(bookmarkService.list(user, entityType, page, size)));
    }
}
