package com.interviewai.notification;

import com.interviewai.common.enums.InterviewStatus;
import com.interviewai.common.enums.NotificationType;
import com.interviewai.domain.Interview;
import com.interviewai.repository.InterviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewReminderScheduler {

    private final InterviewRepository interviewRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 * * * * *")
    public void sendReminders() {
        Instant from = Instant.now();
        Instant to = from.plusSeconds(3600);
        List<Interview> upcoming = interviewRepository
                .findByStatusAndScheduledAtBetween(InterviewStatus.SCHEDULED, from, to);
        for (Interview interview : upcoming) {
            notificationService.send(
                    interview.getCandidate().getId(),
                    NotificationType.INTERVIEW_REMINDER,
                    "Interview Reminder",
                    "Your interview '" + interview.getTitle() + "' starts within the next hour.",
                    "{\"interviewId\":" + interview.getId() + "}");
        }
        if (!upcoming.isEmpty()) {
            log.info("Sent {} interview reminder(s)", upcoming.size());
        }
    }
}
