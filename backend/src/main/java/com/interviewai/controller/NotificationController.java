package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.NotificationResponse;
import com.interviewai.notification.NotificationService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.Map;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "In-app notifications")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "List the current user's notifications")
    public ResponseEntity<ApiResponse<PageResponse<NotificationResponse>>> list(
            @RequestParam(required = false) String type,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort,
            Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(notificationService.list(user.getId(), type, page, size, sort)));
    }

    @GetMapping("/me/unread-count")
    @Operation(summary = "Unread notification count")
    public ResponseEntity<ApiResponse<Map<String, Long>>> unreadCount(Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(Map.of("count", notificationService.unreadCount(user.getId()))));
    }

    @PatchMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<MessageResponse>> markRead(@PathVariable Long id, Principal principal) {
        User user = userService.currentUser(principal.getName());
        notificationService.markRead(id, user.getId());
        return ResponseEntity.ok(ApiResponse.success("Marked as read", MessageResponse.of("Notification marked as read")));
    }

    @PatchMapping("/read-all")
    @Operation(summary = "Mark all notifications as read")
    public ResponseEntity<ApiResponse<MessageResponse>> markAllRead(Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(notificationService.markAllRead(user.getId())));
    }
}
