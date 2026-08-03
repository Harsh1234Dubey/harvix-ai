package com.interviewai.service;

import com.interviewai.audit.AuditTrailService;
import com.interviewai.common.enums.UserRole;
import com.interviewai.common.enums.UserStatus;
import com.interviewai.domain.EmailVerification;
import com.interviewai.domain.Role;
import com.interviewai.domain.User;
import com.interviewai.dto.request.LoginRequest;
import com.interviewai.dto.request.RegisterRequest;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.exception.DuplicateResourceException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private RoleRepository roleRepository;
    @Mock
    private RefreshTokenRepository refreshTokenRepository;
    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock
    private EmailVerificationRepository emailVerificationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;
    @Mock
    private EmailSimulator emailSimulator;
    @Mock
    private NotificationService notificationService;
    @Mock
    private AuditTrailService auditTrailService;
    @Mock
    private HttpServletRequest request;

    @InjectMocks
    private AuthService authService;

    private Role candidateRole;

    @BeforeEach
    void setUp() {
        candidateRole = new Role();
        candidateRole.setCode("CANDIDATE");
    }

    @Test
    void registerRejectsDuplicateEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "Doe", "jane@interviewai.com", "Password@123", "CANDIDATE"),
                request)).isInstanceOf(DuplicateResourceException.class);

        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void registerRejectsAdminRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "Doe", "jane@interviewai.com", "Password@123", "ADMIN"),
                request)).isInstanceOf(com.interviewai.exception.AccessDeniedException.class);
    }

    @Test
    void registerCreatesUserWithVerificationEmail() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(roleRepository.findByCode("CANDIDATE")).thenReturn(Optional.of(candidateRole));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed");

        MessageResponse response = authService.register(
                new RegisterRequest("Jane", "Doe", "jane@interviewai.com", "Password@123", "CANDIDATE"),
                request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        assertThat(userCaptor.getValue().getEmail()).isEqualTo("jane@interviewai.com");
        assertThat(userCaptor.getValue().getStatus()).isEqualTo(UserStatus.PENDING_VERIFICATION);
        assertThat(userCaptor.getValue().getRoles()).extracting(Role::getCode).containsExactly("CANDIDATE");

        ArgumentCaptor<EmailVerification> verificationCaptor = ArgumentCaptor.forClass(EmailVerification.class);
        verify(emailVerificationRepository).save(verificationCaptor.capture());
        assertThat(verificationCaptor.getValue().getToken()).isNotBlank();

        verify(emailSimulator).send(anyString(), anyString(), anyString());
        assertThat(response.message()).contains("Registration successful");
    }

    @Test
    void loginRejectsWrongPasswordAndLocksAccountAfterFiveAttempts() {
        User user = activeUser();
        when(userRepository.findByEmail("jane@interviewai.com")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches(anyString(), anyString())).thenReturn(false);

        for (int i = 1; i <= 5; i++) {
            assertThatThrownBy(() -> authService.login(
                    new LoginRequest("jane@interviewai.com", "wrong", false), request))
                    .isInstanceOf(UnauthorizedException.class);
        }

        assertThat(user.getFailedAttempts()).isEqualTo(5);
        assertThat(user.getLockedUntil()).isNotNull();
        verify(notificationService).send(any(), any(), anyString(), anyString(), any());
    }

    @Test
    void loginLocksOutBlockedAccount() {
        User user = activeUser();
        user.setFailedAttempts(5);
        user.setLockedUntil(Instant.now().plusSeconds(900));
        when(userRepository.findByEmail("jane@interviewai.com")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> authService.login(
                new LoginRequest("jane@interviewai.com", "anything", false), request))
                .isInstanceOf(com.interviewai.exception.AccessDeniedException.class);
    }

    @Test
    void registerRejectsInvalidRole() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Jane", "Doe", "jane@interviewai.com", "Password@123", "SUPERUSER"),
                request)).isInstanceOf(com.interviewai.exception.BadRequestException.class);
    }

    private User activeUser() {
        User user = new User();
        user.setId(7L);
        user.setFirstName("Jane");
        user.setLastName("Doe");
        user.setEmail("jane@interviewai.com");
        user.setPasswordHash("hashed");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.getRoles().add(candidateRole);
        return user;
    }
}
