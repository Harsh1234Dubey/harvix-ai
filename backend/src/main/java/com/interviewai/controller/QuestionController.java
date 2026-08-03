package com.interviewai.controller;

import com.interviewai.common.constants.AppConstants;
import com.interviewai.common.response.ApiResponse;
import com.interviewai.common.response.PageResponse;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateQuestionRequest;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.QuestionBankResponse;
import com.interviewai.service.QuestionService;
import com.interviewai.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import static com.interviewai.common.constants.AppConstants.DEFAULT_PAGE;
import static com.interviewai.common.constants.AppConstants.DEFAULT_SIZE;

@RestController
@RequestMapping(AppConstants.API_BASE_PATH + "/questions")
@RequiredArgsConstructor
@Tag(name = "Question Bank", description = "Search, filter and bookmark interview questions")
public class QuestionController {

    private final QuestionService questionService;
    private final UserService userService;

    @GetMapping
    @Operation(summary = "List/search questions by topic, difficulty and keyword")
    public ResponseEntity<ApiResponse<PageResponse<QuestionBankResponse>>> list(
            @RequestParam(required = false) String topic,
            @RequestParam(required = false) String difficulty,
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = DEFAULT_PAGE) int page,
            @RequestParam(defaultValue = DEFAULT_SIZE) int size) {
        return ResponseEntity.ok(ApiResponse.success(questionService.list(topic, difficulty, q, page, size)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a question by id (increments views)")
    public ResponseEntity<ApiResponse<QuestionBankResponse>> get(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(questionService.get(id)));
    }

    @PostMapping
    @Operation(summary = "Add a question to the bank (admin)")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<QuestionBankResponse>> create(
            @Valid @RequestBody CreateQuestionRequest request) {
        return ResponseEntity.status(201)
                .body(ApiResponse.created("Question added", questionService.create(request)));
    }

    @PostMapping("/{id}/bookmark")
    @Operation(summary = "Toggle bookmark for a question")
    @PreAuthorize("hasRole('CANDIDATE')")
    public ResponseEntity<ApiResponse<MessageResponse>> toggleBookmark(@PathVariable Long id, Principal principal) {
        User user = userService.currentUser(principal.getName());
        return ResponseEntity.ok(ApiResponse.success(questionService.toggleBookmark(user, id)));
    }
}
