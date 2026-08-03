package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.enums.InterviewStatus;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
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
import com.interviewai.service.InterviewService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;
import static com.interviewai.common.constants.AppConstants.SORT_DEFAULT;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/interviews")
@RequiredArgsConstructor
@Tag(name = "Interviews", description = "Scheduling, sessions, questions, answers, feedback")
public class InterviewController {

    private final InterviewService interviewService;
    private final UserService userService;

    @PostMapping
    @Operation(summary = "Schedule an interview (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> schedule(
            @Valid @RequestBody ScheduleInterviewRequest request, Principal principal) {
        User recruiter = userService.currentUser(principal.getName());
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Interview scheduled", interviewService.schedule(request, recruiter)));
    }

    @GetMapping("/me")
    @Operation(summary = "List interviews for the current candidate")
    public ResponseEntity<ApiResponse<PageResponse<InterviewResponse>>> mine(
            Principal principal,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(interviewService.listForCandidate(user.getId(), page, size, sort)));
    }

    @GetMapping("/recruiter")
    @Operation(summary = "List interviews for the current recruiter")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<PageResponse<InterviewResponse>>> recruiterInterviews(
            Principal principal,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size,
            @RequestParam(defaultValue = SORT_DEFAULT) String sort) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(interviewService.listForRecruiter(user.getId(), page, size, sort)));
    }

    @GetMapping("/{uuid}")
    @Operation(summary = "Get an interview by public id")
    public ResponseEntity<ApiResponse<InterviewResponse>> get(@PathVariable UUID uuid) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.get(uuid)));
    }

    @PatchMapping("/{uuid}/reschedule")
    @Operation(summary = "Reschedule an interview (recruiter)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> reschedule(
            @PathVariable UUID uuid, @RequestParam Instant newTime) {
        return ResponseEntity.ok(ApiResponse.success("Interview rescheduled", interviewService.reschedule(uuid, newTime)));
    }

    @PatchMapping("/{uuid}/status")
    @Operation(summary = "Update interview status (recruiter/admin)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> status(
            @PathVariable UUID uuid, @RequestParam InterviewStatus status) {
        return ResponseEntity.ok(ApiResponse.success("Status updated", interviewService.updateStatus(uuid, status)));
    }

    @PatchMapping("/{uuid}/score")
    @Operation(summary = "Record score and hiring recommendation (recruiter)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<InterviewResponse>> score(
            @PathVariable UUID uuid, @RequestParam BigDecimal score,
            @RequestParam String recommendation, @RequestParam(required = false) String summary) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.recordScore(uuid, score, recommendation, summary)));
    }

    @PostMapping("/slots")
    @Operation(summary = "Create availability slots (recruiter)")
    @PreAuthorize("hasAnyRole('RECRUITER','ADMIN')")
    public ResponseEntity<ApiResponse<MessageResponse>> createSlots(
            @Valid @RequestBody List<CreateInterviewSlotRequest> slots, Principal principal) {
        User recruiter = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(interviewService.createSlots(recruiter.getId(), slots)));
    }

    @GetMapping("/slots")
    @Operation(summary = "List available slots for a recruiter")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> slots(
            @RequestParam Long recruiterId, @RequestParam Instant from, @RequestParam Instant to) {
        List<Map<String, Object>> slots = interviewService.listSlots(recruiterId, from, to).stream()
                .map(slot -> Map.<String, Object>of(
                        "id", slot.getId(),
                        "startsAt", slot.getStartsAt().toString(),
                        "endsAt", slot.getEndsAt().toString(),
                        "booked", slot.isBooked()))
                .toList();
        return ResponseEntity.ok(ApiResponse.success(slots));
    }

    @PostMapping("/sessions")
    @Operation(summary = "Start an AI mock interview session (candidate)")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<Map<String, Object>>> startSession(
            @Valid @RequestBody StartInterviewSessionRequest request, Principal principal) {
        User candidate = userService.currentUser(principal.getName());
        InterviewSession session = interviewService.startSession(candidate.getId(), request);
        return ResponseEntity.status(201).body(ApiResponse.created("Session started", Map.of(
                "sessionId", session.getId(),
                "uuid", session.getUuid(),
                "skill", session.getSkill(),
                "difficulty", session.getDifficulty())));
    }

    @GetMapping("/sessions/{id}")
    @Operation(summary = "Get session details")
    public ResponseEntity<ApiResponse<Map<String, Object>>> session(@PathVariable Long id) {
        InterviewSession session = interviewService.getSession(id);
        return ResponseEntity.ok(ApiResponse.success(Map.of(
                "sessionId", session.getId(),
                "uuid", session.getUuid(),
                "skill", session.getSkill(),
                "difficulty", session.getDifficulty(),
                "status", session.getStatus().name(),
                "totalQuestions", session.getTotalQuestions(),
                "answeredQuestions", session.getAnsweredQuestions())));
    }

    @GetMapping("/sessions/{id}/questions")
    @Operation(summary = "List questions in a session")
    public ResponseEntity<ApiResponse<List<QuestionResponse>>> questions(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.questions(id)));
    }

    @PostMapping("/sessions/{id}/answers")
    @Operation(summary = "Submit an answer for a question")
    public ResponseEntity<ApiResponse<MessageResponse>> answer(
            @PathVariable Long id, @Valid @RequestBody SubmitAnswerRequest request) {
        return ResponseEntity.ok(ApiResponse.success(interviewService.submitAnswer(id, request)));
    }

    @PostMapping("/sessions/{id}/feedback")
    @Operation(summary = "Generate AI feedback for a completed session")
    public ResponseEntity<ApiResponse<InterviewFeedbackResponse>> feedback(@PathVariable Long id) {
        InterviewFeedbackResponse feedback = interviewService.generateFeedback(id);
        return ResponseEntity.status(201).body(ApiResponse.created("AI feedback generated", feedback));
    }
}
