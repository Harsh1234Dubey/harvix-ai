package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.dto.request.ChangePasswordRequest;
import com.interviewai.dto.request.UpdateProfileRequest;
import com.interviewai.dto.request.UpdateUserStatusRequest;
import com.interviewai.dto.response.DashboardStats;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.UserResponse;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Profile, dashboard and admin user management")
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.getProfile(principal.getName())));
    }

    @PutMapping("/me")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<UserResponse>> updateProfile(
            Principal principal, @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", userService.updateProfile(principal.getName(), request)));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Change the current user's password")
    public ResponseEntity<ApiResponse<MessageResponse>> changePassword(
            Principal principal, @Valid @RequestBody ChangePasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(userService.changePassword(principal.getName(), request)));
    }

    @GetMapping("/me/dashboard")
    @Operation(summary = "Candidate dashboard statistics")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<DashboardStats>> dashboard(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(userService.candidateDashboard(principal.getName())));
    }

    @GetMapping
    @Operation(summary = "List all users (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<UserResponse>>> listUsers(
            @RequestParam(required = false) String search,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        return ResponseEntity.ok(ApiResponse.success(userService.listUsers(search, page, size, sort)));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Activate, block or deactivate a user (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserResponse>> updateStatus(
            @PathVariable Long id, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(ApiResponse.success("User status updated", userService.updateStatus(id, request)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a user (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<MessageResponse>> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("User deleted", MessageResponse.of("User deleted")));
    }
}
