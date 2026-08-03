package com.interviewai.service;

import com.interviewai.common.enums.NotificationType;
import com.interviewai.domain.CodingStreak;
import com.interviewai.domain.LeaderboardEntry;
import com.interviewai.domain.User;
import com.interviewai.domain.XpTransaction;
import com.interviewai.dto.response.LeaderboardResponse;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.notification.NotificationService;
import com.interviewai.repository.CodingStreakRepository;
import com.interviewai.repository.LeaderboardEntryRepository;
import com.interviewai.repository.UserRepository;
import com.interviewai.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class GamificationService {

    public static final int XP_PER_LEVEL = 100;

    private final XpTransactionRepository xpTransactionRepository;
    private final LeaderboardEntryRepository leaderboardEntryRepository;
    private final CodingStreakRepository codingStreakRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    @Transactional
    public void awardXp(Long userId, int amount, String reason, Long referenceId) {
        XpTransaction transaction = new XpTransaction();
        transaction.setUser(new User());
        transaction.getUser().setId(userId);
        transaction.setXpChange(amount);
        transaction.setReason(reason);
        transaction.setReferenceId(referenceId);
        xpTransactionRepository.save(transaction);

        int totalXp = xpTransactionRepository.sumXp(userId);
        LeaderboardEntry entry = leaderboardEntryRepository.findByUserIdAndPeriod(userId, "ALL_TIME")
                .orElseGet(() -> {
                    LeaderboardEntry created = new LeaderboardEntry();
                    created.setUser(new User());
                    created.getUser().setId(userId);
                    created.setPeriod("ALL_TIME");
                    return created;
                });
        entry.setXpTotal(totalXp);
        leaderboardEntryRepository.save(entry);
    }

    @Transactional
    public void updateStreak(Long userId) {
        CodingStreak streak = codingStreakRepository.findByUserId(userId).orElseGet(() -> {
            CodingStreak created = new CodingStreak();
            created.setUser(new User());
            created.getUser().setId(userId);
            created.setCurrentStreak(0);
            created.setLongestStreak(0);
            return created;
        });
        LocalDate today = LocalDate.now();
        if (streak.getLastActiveDate() == null) {
            streak.setCurrentStreak(1);
        } else if (streak.getLastActiveDate().equals(today)) {
            // already counted today
        } else if (streak.getLastActiveDate().equals(today.minusDays(1))) {
            streak.setCurrentStreak(streak.getCurrentStreak() + 1);
        } else {
            streak.setCurrentStreak(1);
        }
        streak.setLongestStreak(Math.max(streak.getLongestStreak(), streak.getCurrentStreak()));
        streak.setLastActiveDate(today);
        codingStreakRepository.save(streak);

        if (streak.getCurrentStreak() % 7 == 0) {
            notificationService.send(userId, NotificationType.ACHIEVEMENT, "Coding streak",
                    "You hit a " + streak.getCurrentStreak() + "-day coding streak. Keep it up!",
                    "{\"streak\":" + streak.getCurrentStreak() + "}");
        }
    }

    @Transactional(readOnly = true)
    public List<LeaderboardResponse> leaderboard(String period, int limit) {
        String safePeriod = period != null && !period.isBlank() ? period.toUpperCase() : "ALL_TIME";
        return leaderboardEntryRepository
                .findByPeriodOrderByXpTotalDesc(safePeriod, Pageable.ofSize(Math.min(Math.max(limit, 1), 100)))
                .stream()
                .map(entry -> {
                    User user = userRepository.findById(entry.getUser().getId())
                            .orElseThrow(() -> ResourceNotFoundException.of("User", entry.getUser().getId()));
                    return new LeaderboardResponse(
                            "#" + (rank(safePeriod, entry.getXpTotal())),
                            user.getId(),
                            user.getFirstName() + " " + user.getLastName(),
                            user.getEmail(),
                            user.getAvatarUrl(),
                            entry.getXpTotal());
                })
                .toList();
    }

    @Transactional(readOnly = true)
    public int rank(String period, int xp) {
        List<LeaderboardEntry> entries = leaderboardEntryRepository
                .findByPeriodOrderByXpTotalDesc(period, Pageable.unpaged());
        int position = 1;
        for (LeaderboardEntry entry : entries) {
            if (entry.getXpTotal() > xp) {
                position++;
            }
        }
        return position;
    }
}
