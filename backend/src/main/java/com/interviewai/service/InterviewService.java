package com.interviewai.service;

import com.interviewai.ai.AiFeedbackService;
import com.interviewai.ai.AiFeedbackResult;
import com.interviewai.ai.AiQuestionService;
import com.interviewai.ai.GeneratedQuestion;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.InterviewStatus;
import com.interviewai.common.enums.InterviewType;
import com.interviewai.common.enums.NotificationType;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Interview;
import com.interviewai.domain.InterviewAnswer;
import com.interviewai.domain.InterviewFeedback;
import com.interviewai.domain.InterviewQuestion;
import com.interviewai.domain.InterviewSession;
import com.interviewai.domain.InterviewSlot;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateInterviewSlotRequest;
import com.interviewai.dto.request.ScheduleInterviewRequest;
import com.interviewai.dto.request.StartInterviewSessionRequest;
import com.interviewai.dto.request.SubmitAnswerRequest;
import com.interviewai.dto.response.InterviewFeedbackResponse;
import com.interviewai.dto.response.InterviewResponse;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.QuestionResponse;
import com.interviewai.exception.BadRequestException;
import com.interviewai.exception.ResourceNotFoundException;
import com.interviewai.notification.NotificationService;
import com.interviewai.repository.InterviewAnswerRepository;
import com.interviewai.repository.InterviewFeedbackRepository;
import com.interviewai.repository.InterviewQuestionRepository;
import com.interviewai.repository.InterviewRepository;
import com.interviewai.repository.InterviewSessionRepository;
import com.interviewai.repository.InterviewSlotRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InterviewService {

    private static final int DEFAULT_SESSION_QUESTIONS = 5;

    private final InterviewRepository interviewRepository;
    private final InterviewSlotRepository interviewSlotRepository;
    private final InterviewSessionRepository sessionRepository;
    private final InterviewQuestionRepository questionRepository;
    private final InterviewAnswerRepository answerRepository;
    private final InterviewFeedbackRepository feedbackRepository;
    private final NotificationService notificationService;
    private final AiQuestionService aiQuestionService;
    private final AiFeedbackService aiFeedbackService;

    @Transactional
    public InterviewResponse schedule(ScheduleInterviewRequest request, User recruiter) {
        Interview interview = new Interview();
        interview.setRecruiter(recruiter);
        if (request.applicationId() != null) {
            interview.setApplication(new com.interviewai.domain.Application());
            interview.getApplication().setId(request.applicationId());
        }
        interview.setCandidate(new User());
        interview.getCandidate().setId(request.candidateId());
        interview.setTitle(request.title() != null ? request.title() : "Interview");
        interview.setType(request.type() != null ? request.type() : InterviewType.TECHNICAL);
        interview.setScheduledAt(request.scheduledAt());
        if (request.durationMin() != null) interview.setDurationMin(request.durationMin());
        interview.setLocation(request.location());
        interview.setMeetingLink(request.meetingLink());
        interview.setDifficulty(request.difficulty());
        Interview saved = interviewRepository.save(interview);

        notificationService.send(request.candidateId(), NotificationType.INTERVIEW_REMINDER,
                "Interview scheduled",
                "You have an interview scheduled: " + interview.getTitle(),
                "{\"interviewId\":" + saved.getId() + "}");
        return InterviewResponse.from(saved);
    }

    @Transactional
    public InterviewResponse reschedule(UUID uuid, Instant newTime) {
        Interview interview = findByUuid(uuid);
        interview.setScheduledAt(newTime);
        interview.setStatus(InterviewStatus.RESCHEDULED);
        Interview saved = interviewRepository.save(interview);
        notificationService.send(interview.getCandidate().getId(), NotificationType.INTERVIEW_REMINDER,
                "Interview rescheduled",
                "Your interview '" + interview.getTitle() + "' has been rescheduled.",
                "{\"interviewId\":" + saved.getId() + "}");
        return InterviewResponse.from(saved);
    }

    @Transactional
    public InterviewResponse updateStatus(UUID uuid, InterviewStatus status) {
        Interview interview = findByUuid(uuid);
        interview.setStatus(status);
        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional
    public InterviewResponse recordScore(UUID uuid, BigDecimal score, String recommendation, String summary) {
        Interview interview = findByUuid(uuid);
        interview.setScore(score);
        interview.setHiringRecommendation(recommendation);
        interview.setFeedbackSummary(summary);
        interview.setStatus(InterviewStatus.COMPLETED);
        return InterviewResponse.from(interviewRepository.save(interview));
    }

    @Transactional(readOnly = true)
    public PageResponse<InterviewResponse> listForCandidate(Long candidateId, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Interview> interviews = interviewRepository.findByCandidateId(candidateId, pageable);
        return PageResponse.from(interviews, interviews.stream().map(InterviewResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public PageResponse<InterviewResponse> listForRecruiter(Long recruiterId, int page, int size, String sort) {
        Pageable pageable = PageableUtils.build(page, size, sort);
        Page<Interview> interviews = interviewRepository.findByRecruiterId(recruiterId, pageable);
        return PageResponse.from(interviews, interviews.stream().map(InterviewResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public InterviewResponse get(UUID uuid) {
        return InterviewResponse.from(findByUuid(uuid));
    }

    @Transactional
    public MessageResponse createSlots(Long recruiterId, List<CreateInterviewSlotRequest> slots) {
        for (CreateInterviewSlotRequest slotRequest : slots) {
            if (!slotRequest.endsAt().isAfter(slotRequest.startsAt())) {
                throw new BadRequestException("Slot end time must be after start time");
            }
            if (interviewSlotRepository.existsByRecruiterIdAndStartsAtAndEndsAt(
                    recruiterId, slotRequest.startsAt(), slotRequest.endsAt())) {
                throw new BadRequestException("Overlapping slot already exists");
            }
            InterviewSlot slot = new InterviewSlot();
            slot.setRecruiter(new User());
            slot.getRecruiter().setId(recruiterId);
            slot.setStartsAt(slotRequest.startsAt());
            slot.setEndsAt(slotRequest.endsAt());
            interviewSlotRepository.save(slot);
        }
        return MessageResponse.of(slots.size() + " slot(s) created");
    }

    @Transactional(readOnly = true)
    public List<InterviewSlot> listSlots(Long recruiterId, Instant from, Instant to) {
        return interviewSlotRepository.findByRecruiterIdAndStartsAtBetween(recruiterId, from, to);
    }

    @Transactional
    public InterviewSession startSession(Long candidateId, StartInterviewSessionRequest request) {
        InterviewSession session = new InterviewSession();
        if (request.interviewId() != null) {
            session.setInterview(findById(request.interviewId()));
        }
        session.setCandidate(new User());
        session.getCandidate().setId(candidateId);
        session.setSkill(request.skill());
        session.setDifficulty(request.difficulty() != null ? request.difficulty() : Difficulty.MEDIUM);
        session.setStatus(InterviewStatus.IN_PROGRESS);
        session.setStartedAt(Instant.now());
        session = sessionRepository.save(session);

        List<GeneratedQuestion> questions = aiQuestionService.generate(
                request.skill(), session.getDifficulty(), DEFAULT_SESSION_QUESTIONS);
        int order = 0;
        for (GeneratedQuestion generated : questions) {
            addQuestion(session.getId(), generated.question(), generated.topic(),
                    safeDifficulty(generated.difficulty(), session.getDifficulty()),
                    generated.category(), false, null, order++);
        }
        return session;
    }

    private Difficulty safeDifficulty(String value, Difficulty fallback) {
        if (value == null) {
            return fallback;
        }
        try {
            return Difficulty.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    @Transactional(readOnly = true)
    public InterviewSession getSession(Long sessionId) {
        return sessionRepository.findById(sessionId)
                .orElseThrow(() -> ResourceNotFoundException.of("InterviewSession", sessionId));
    }

    @Transactional(readOnly = true)
    public List<QuestionResponse> questions(Long sessionId) {
        return questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId).stream()
                .map(QuestionResponse::from)
                .toList();
    }

    @Transactional
    public QuestionResponse addQuestion(Long sessionId, String text, String topic, Difficulty difficulty,
                                        String category, boolean followUp, Long followUpOf, int orderIndex) {
        InterviewSession session = getSession(sessionId);
        InterviewQuestion question = new InterviewQuestion();
        question.setSession(session);
        question.setQuestionText(text);
        question.setTopic(topic);
        question.setDifficulty(difficulty);
        question.setCategory(category);
        question.setFollowUp(followUp);
        if (followUpOf != null) {
            question.setFollowUpOf(new InterviewQuestion());
            question.getFollowUpOf().setId(followUpOf);
        }
        question.setOrderIndex(orderIndex);
        questionRepository.save(question);
        session.setTotalQuestions(session.getTotalQuestions() + 1);
        sessionRepository.save(session);
        return QuestionResponse.from(question);
    }

    @Transactional
    public MessageResponse submitAnswer(Long sessionId, SubmitAnswerRequest request) {
        InterviewQuestion question = questionRepository.findById(request.questionId())
                .orElseThrow(() -> ResourceNotFoundException.of("InterviewQuestion", request.questionId()));
        InterviewAnswer answer = new InterviewAnswer();
        answer.setQuestion(question);
        answer.setAnswerText(request.answerText());
        answer.setVoiceToText(request.voiceToText());
        answer.setSkipped(Boolean.TRUE.equals(request.skipped()));
        answerRepository.save(answer);

        InterviewSession session = question.getSession();
        session.setAnsweredQuestions(session.getAnsweredQuestions() + 1);
        sessionRepository.save(session);
        return MessageResponse.of("Answer recorded");
    }

    @Transactional
    public InterviewFeedbackResponse generateFeedback(Long sessionId) {
        InterviewSession session = getSession(sessionId);
        session.setStatus(InterviewStatus.COMPLETED);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        List<InterviewQuestion> questions = questionRepository.findBySessionIdOrderByOrderIndexAsc(sessionId);
        List<InterviewAnswer> answers = answerRepository.findByQuestionSessionIdOrderByAnsweredAtAsc(sessionId);
        AiFeedbackResult result = aiFeedbackService.generate(session, questions, answers);

        InterviewFeedback feedback = feedbackRepository.findBySessionId(sessionId)
                .orElse(new InterviewFeedback());
        feedback.setSession(session);
        feedback.setOverallScore(result.overall());
        feedback.setCommunication(result.communication());
        feedback.setConfidence(result.confidence());
        feedback.setTechnicalKnowledge(result.technical());
        feedback.setGrammar(result.grammar());
        feedback.setFluency(result.fluency());
        feedback.setKeywordMatch(result.keywordMatch());
        feedback.setSpeakingSpeed(result.speakingSpeed());
        feedback.setStrengthsJson(toJson(result.strengths()));
        feedback.setWeaknessesJson(toJson(result.weaknesses()));
        feedback.setLearningSuggestions(result.suggestions());
        feedback.setHiringRecommendation(result.recommendation());
        feedback.setDetailedJson(toJson(result.detailed()));
        return InterviewFeedbackResponse.from(feedbackRepository.save(feedback));
    }

    private String toJson(Object value) {
        try {
            return com.interviewai.common.util.JsonUtil.write(value);
        } catch (Exception e) {
            return "[]";
        }
    }

    @Transactional(readOnly = true)
    public Interview findByUuid(UUID uuid) {
        return interviewRepository.findByUuid(uuid)
                .orElseThrow(() -> ResourceNotFoundException.of("Interview", uuid));
    }

    @Transactional(readOnly = true)
    public Interview findById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> ResourceNotFoundException.of("Interview", id));
    }
}
