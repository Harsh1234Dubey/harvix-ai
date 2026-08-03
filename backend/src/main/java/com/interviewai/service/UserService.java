package com.interviewai.service;

import com.interviewai.common.enums.UserStatus;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.User;
import com.interviewai.dto.request.ChangePasswordRequest;
import com.interviewai.dto.request.UpdateProfileRequest;
import com.interviewai.dto.request.UpdateUserStatusRequest;
import com.interviewai.dto.response.DashboardStats;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.UserResponse;
import com.interviewai.exception.BadRequestException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.exception.UnauthorizedException;
import com.interviewai.repository.ApplicationRepository;
import com.interviewai.repository.AtsReportRepository;
import com.interviewai.repository.BookmarkRepository;
import com.interviewai.repository.CodingSubmissionRepository;
import com.interviewai.repository.InterviewRepository;
import com.interviewai.repository.NotificationRepository;
import com.interviewai.repository.UserRepository;
import com.interviewai.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationRepository applicationRepository;
    private final BookmarkRepository bookmarkRepository;
    private final InterviewRepository interviewRepository;
    private final CodingSubmissionRepository codingSubmissionRepository;
    private final NotificationRepository notificationRepository;
    private final XpTransactionRepository xpTransactionRepository;
    private final AtsReportRepository atsReportRepository;

    @Transactional(readOnly = true)
    public UserResponse getProfile(String email) {
        return UserResponse.from(currentUser(email));
    }

    @Transactional
    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = currentUser(email);
        if (request.firstName() != null && !request.firstName().isBlank()) {
            user.setFirstName(request.firstName());
        }
        if (request.lastName() != null && !request.lastName().isBlank()) {
            user.setLastName(request.lastName());
        }
        if (request.phone() != null) {
            user.setPhone(request.phone());
        }
        if (request.avatarUrl() != null) {
            user.setAvatarUrl(request.avatarUrl());
        }
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public MessageResponse changePassword(String email, ChangePasswordRequest request) {
        User user = currentUser(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPasswordHash())) {
            throw new BadRequestException("Current password is incorrect");
        }
        userRepository.updatePassword(user.getId(), passwordEncoder.encode(request.newPassword()));
        return MessageResponse.of("Password updated successfully");
    }

    @Transactional(readOnly = true)
    public DashboardStats candidateDashboard(String email) {
        User user = currentUser(email);
        Long id = user.getId();
        int xp = xpTransactionRepository.sumXp(id);
        var interviews = interviewRepository.findByCandidateId(id, Pageable.unpaged());
        long completed = interviews.getContent().stream()
                .filter(i -> i.getStatus() == com.interviewai.common.enums.InterviewStatus.COMPLETED)
                .count();
        Double bestResumeScore = atsReportRepository.findFirstByCandidateIdOrderByScoreDesc(id)
                .map(report -> report.getScore().doubleValue())
                .orElse(null);
        return new DashboardStats(
                applicationRepository.countByCandidateId(id),
                bookmarkRepository.countByUserIdAndEntityType(id, "JOB"),
                interviews.getTotalElements(),
                completed,
                codingSubmissionRepository.findByCandidateId(id, Pageable.unpaged()).getTotalElements(),
                xp,
                xp / 100 + 1,
                notificationRepository.countByUserIdAndReadFalse(id),
                null,
                bestResumeScore);
    }

    @Transactional(readOnly = true)
    public PageResponse<UserResponse> listUsers(String search, int page, int size, String sort) {
        Page<User> users;
        Pageable pageable = PageableUtils.build(page, size, sort);
        if (search != null && !search.isBlank()) {
            users = userRepository.findAll(pageable);
        } else {
            users = userRepository.findAll(pageable);
        }
        return PageResponse.from(users, users.stream().map(UserResponse::from).toList());
    }

    @Transactional
    public UserResponse updateStatus(Long userId, UpdateUserStatusRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        user.setStatus(request.status());
        return UserResponse.from(userRepository.save(user));
    }

    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> ResourceNotFoundException.of("User", userId));
        userRepository.delete(user);
    }

    @Transactional(readOnly = true)
    public User currentUser(String email) {
        if (email == null) {
            throw new UnauthorizedException("Not authenticated");
        }
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UnauthorizedException("User not found"));
    }
}
