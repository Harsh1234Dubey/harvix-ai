package com.interviewai.notification;

import com.interviewai.common.enums.NotificationType;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Notification;
import com.interviewai.domain.User;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.NotificationResponse;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    @Async
    public void send(Long userId, NotificationType type, String title, String message, String dataJson) {
        Notification notification = new Notification();
        User user = new User();
        user.setId(userId);
        notification.setUser(user);
        notification.setType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setDataJson(dataJson);
        notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public PageResponse<NotificationResponse> list(Long userId, String type, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Notification> notifications;
        if (type != null && !type.isBlank()) {
            notifications = notificationRepository.findByUserIdAndType(userId, NotificationType.valueOf(type.toUpperCase()), pageable);
        } else {
            notifications = notificationRepository.findByUserId(userId, pageable);
        }
        return PageResponse.from(notifications, notifications.stream().map(NotificationResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long userId) {
        return notificationRepository.countByUserIdAndReadFalse(userId);
    }

    @Transactional
    public void markRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> ResourceNotFoundException.of("Notification", notificationId));
        if (!notification.getUser().getId().equals(userId)) {
            throw new com.interviewai.exception.AccessDeniedException("Cannot modify another user's notification");
        }
        notification.setRead(true);
        notification.setReadAt(java.time.Instant.now());
        notificationRepository.save(notification);
    }

    @Transactional
    public MessageResponse markAllRead(Long userId) {
        notificationRepository.markAllRead(userId);
        return MessageResponse.of("All notifications marked as read");
    }
}
