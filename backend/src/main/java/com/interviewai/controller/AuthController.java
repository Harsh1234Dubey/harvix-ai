package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.dto.request.ForgotPasswordRequest;
import com.interviewai.dto.request.LoginRequest;
import com.interviewai.dto.request.RefreshTokenRequest;
import com.interviewai.dto.request.RegisterRequest;
import com.interviewai.dto.request.ResetPasswordRequest;
import com.interviewai.dto.request.VerifyEmailRequest;
import com.interviewai.dto.response.AuthResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.UserResponse;
import com.interviewai.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, login, token refresh, password flows")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register a candidate or recruiter")
    public ResponseEntity<ApiResponse<MessageResponse>> register(
            @Valid @RequestBody RegisterRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Account created", authService.register(request, httpRequest)));
    }

    @PostMapping("/login")
    @Operation(summary = "Login and obtain access + refresh tokens")
    public ResponseEntity<ApiResponse<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest) {
        return ResponseEntity.ok(ApiResponse.success("Login successful", authService.login(request, httpRequest)));
    }

    @PostMapping("/refresh")
    @Operation(summary = "Exchange a refresh token for a new token pair")
    public ResponseEntity<ApiResponse<AuthResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Tokens refreshed", authService.refresh(request)));
    }

    @PostMapping("/logout")
    @Operation(summary = "Revoke all refresh tokens for the current user")
    public ResponseEntity<ApiResponse<MessageResponse>> logout(Principal principal) {
        authService.logout(principal != null ? principal.getName() : null);
        return ResponseEntity.ok(ApiResponse.success("Logged out", MessageResponse.of("Logged out successfully")));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request a password reset token (simulated email)")
    public ResponseEntity<ApiResponse<MessageResponse>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.forgotPassword(request)));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password using a reset token")
    public ResponseEntity<ApiResponse<MessageResponse>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.resetPassword(request)));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email address using the verification token")
    public ResponseEntity<ApiResponse<MessageResponse>> verifyEmail(
            @Valid @RequestBody VerifyEmailRequest request) {
        return ResponseEntity.ok(ApiResponse.success(authService.verifyEmail(request.token())));
    }

    @GetMapping("/me")
    @Operation(summary = "Get the authenticated user's profile")
    public ResponseEntity<ApiResponse<UserResponse>> me(Principal principal) {
        return ResponseEntity.ok(ApiResponse.success(authService.me(principal.getName())));
    }
}
