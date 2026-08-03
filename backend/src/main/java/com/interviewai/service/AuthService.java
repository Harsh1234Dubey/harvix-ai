package com.interviewai.service;

import com.interviewai.audit.AuditTrailService;
import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.enums.AuditAction;
import com.interviewai.common.enums.NotificationType;
import com.interviewai.common.enums.UserRole;
import com.interviewai.common.enums.UserStatus;
import com.interviewai.domain.EmailVerification;
import com.interviewai.domain.PasswordResetToken;
import com.interviewai.domain.RefreshToken;
import com.interviewai.domain.Role;
import com.interviewai.domain.User;
import com.interviewai.dto.request.ForgotPasswordRequest;
import com.interviewai.dto.request.LoginRequest;
import com.interviewai.dto.request.RefreshTokenRequest;
import com.interviewai.dto.request.RegisterRequest;
import com.interviewai.dto.request.ResetPasswordRequest;
import com.interviewai.dto.response.AuthResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.UserResponse;
import com.interviewai.exception.AccessDeniedException;
import com.interviewai.exception.BadRequestException;
import com.interviewai.exception.DuplicateResourceException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.exception.TokenExpiredException;
import com.interviewai.exception.UnauthorizedException;
import com.interviewai.notification.EmailSimulator;
import com.interviewai.notification.NotificationService;
import com.interviewai.repository.EmailVerificationRepository;
import com.interviewai.repository.PasswordResetTokenRepository;
import com.interviewai.repository.RefreshTokenRepository;
import com.interviewai.repository.RoleRepository;
import com.interviewai.repository.UserRepository;
import com.interviewai.security.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Duration VERIFY_TOKEN_TTL = Duration.ofHours(24);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(30);
    private static final int MAX_FAILED_ATTEMPTS = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailVerificationRepository emailVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final EmailSimulator emailSimulator;
    private final NotificationService notificationService;
    private final AuditTrailService auditTrailService;

    @Transactional
    public MessageResponse register(RegisterRequest request, HttpServletRequest httpRequest) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("An account already exists for email: " + request.email());
        }
        UserRole roleEnum;
        try {
            roleEnum = UserRole.valueOf(request.role().toUpperCase());
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException("Invalid role. Allowed: CANDIDATE, RECRUITER");
        }
        if (roleEnum == UserRole.ADMIN) {
            throw new AccessDeniedException("Admin accounts cannot be self-registered");
        }
        Role role = roleRepository.findByCode(roleEnum.name())
                .orElseThrow(() -> ResourceNotFoundException.of("Role", roleEnum.name()));

        User user = new User();
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setEmail(request.email().toLowerCase().trim());
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setStatus(UserStatus.PENDING_VERIFICATION);
        user.getRoles().add(role);
        userRepository.save(user);

        String token = UUID.randomUUID().toString();
        EmailVerification verification = new EmailVerification();
        verification.setUser(user);
        verification.setToken(token);
        verification.setExpiresAt(Instant.now().plus(VERIFY_TOKEN_TTL));
        emailVerificationRepository.save(verification);

        emailSimulator.send(user.getEmail(), "Verify your InterView AI account",
                "Your verification token: " + token + "\nAPI: POST /api/v1/auth/verify-email");

        auditTrailService.record(user.getId(), AuditAction.CREATE, "User", String.valueOf(user.getId()), null, null);
        return MessageResponse.of("Registration successful. Check your (simulated) email to verify your account.");
    }

    @Transactional
    public AuthResponse login(LoginRequest request, HttpServletRequest httpRequest) {
        User user = userRepository.findByEmail(request.email().toLowerCase().trim())
                .orElseThrow(() -> new UnauthorizedException("Invalid email or password"));

        if (user.getStatus() == UserStatus.BLOCKED || (user.getLockedUntil() != null && user.getLockedUntil().isAfter(Instant.now()))) {
            throw new AccessDeniedException("Account is locked. Try again later.");
        }
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AccessDeniedException("Account is inactive. Contact support.");
        }
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            incrementFailedAttempts(user);
            throw new UnauthorizedException("Invalid email or password");
        }
        user.setFailedAttempts(0);
        user.setLockedUntil(null);
        user.setLastLoginAt(Instant.now());
        user.setLastLoginIp(httpRequest.getRemoteAddr());
        userRepository.save(user);

        auditTrailService.record(user.getId(), AuditAction.LOGIN, "User", String.valueOf(user.getId()), null, null);
        return buildAuthResponse(user, request.rememberMe());
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository.findByToken(request.refreshToken())
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (stored.isRevoked() || stored.getExpiresAt().isBefore(Instant.now())) {
            throw new UnauthorizedException("Refresh token is revoked or expired");
        }
        User user = stored.getUser();
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AccessDeniedException("Account is not active");
        }
        stored.setRevoked(true);
        refreshTokenRepository.save(stored);
        return buildAuthResponse(user, false);
    }

    @Transactional
    public void logout(String email) {
        if (email == null) {
            return;
        }
        User user = userRepository.findByEmail(email).orElse(null);
        if (user != null) {
            refreshTokenRepository.revokeAllForUser(user.getId());
            auditTrailService.record(user.getId(), AuditAction.LOGOUT, "User", String.valueOf(user.getId()), null, null);
        }
    }

    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        userRepository.findByEmail(request.email().toLowerCase().trim()).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = new PasswordResetToken();
            resetToken.setUser(user);
            resetToken.setToken(token);
            resetToken.setExpiresAt(Instant.now().plus(RESET_TOKEN_TTL));
            passwordResetTokenRepository.save(resetToken);
            emailSimulator.send(user.getEmail(), "Reset your InterView AI password",
                    "Your reset token: " + token + "\nAPI: POST /api/v1/auth/reset-password");
        });
        return MessageResponse.of("If the email exists, a reset link has been sent.");
    }

    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(request.token())
                .orElseThrow(() -> new UnauthorizedException("Invalid reset token"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Reset token is expired or already used");
        }
        User user = resetToken.getUser();
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);
        refreshTokenRepository.revokeAllForUser(user.getId());
        auditTrailService.record(user.getId(), AuditAction.PASSWORD_RESET, "User", String.valueOf(user.getId()), null, null);
        notificationService.send(user.getId(), NotificationType.SYSTEM, "Password changed",
                "Your password was successfully reset.", null);
        return MessageResponse.of("Password reset successful. Please sign in again.");
    }

    @Transactional
    public MessageResponse verifyEmail(String token) {
        EmailVerification verification = emailVerificationRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid verification token"));
        if (verification.getVerifiedAt() != null) {
            return MessageResponse.of("Email already verified.");
        }
        if (verification.getExpiresAt().isBefore(Instant.now())) {
            throw new TokenExpiredException("Verification token has expired. Please request a new one.");
        }
        verification.setVerifiedAt(Instant.now());
        emailVerificationRepository.save(verification);
        userRepository.markEmailVerified(verification.getUser().getId());
        notificationService.send(verification.getUser().getId(), NotificationType.SYSTEM, "Welcome!",
                "Your email is verified. Complete your profile to get started.", null);
        return MessageResponse.of("Email verified successfully.");
    }

    public UserResponse me(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
        return UserResponse.from(user);
    }

    private AuthResponse buildAuthResponse(User user, boolean rememberMe) {
        String accessToken = jwtService.generateAccessToken(user);
        String refreshToken = jwtService.generateRefreshToken(user, rememberMe);

        RefreshToken stored = new RefreshToken();
        stored.setUser(user);
        stored.setToken(refreshToken);
        stored.setExpiresAt(Instant.now().plus(rememberMe ? Duration.ofDays(30) : Duration.ofDays(7)));
        refreshTokenRepository.save(stored);

        return new AuthResponse(accessToken, refreshToken, "Bearer",
                jwtService.parse(accessToken).getExpiration().getTime() - System.currentTimeMillis(),
                rememberMe, UserResponse.from(user));
    }

    private void incrementFailedAttempts(User user) {
        int attempts = user.getFailedAttempts() + 1;
        user.setFailedAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(Duration.ofMinutes(15)));
            notificationService.send(user.getId(), NotificationType.SYSTEM, "Account locked",
                    "Too many failed attempts. Account locked for 15 minutes.", null);
        }
        userRepository.save(user);
    }
}
