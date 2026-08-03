package com.interviewai.service;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.NotificationType;
import com.interviewai.common.response.PageResponse;
import com.interviewai.common.util.PageableUtils;
import com.interviewai.domain.Question;
import com.interviewai.domain.User;
import com.interviewai.dto.request.CreateQuestionRequest;
import com.interviewai.dto.response.MessageResponse;
import com.interviewai.dto.response.QuestionBankResponse;
import com.interviewai.exception.DuplicateResourceException;
import com.interviewai.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final BookmarkService bookmarkService;

    @Transactional(readOnly = true)
    public PageResponse<QuestionBankResponse> list(String topic, String difficulty, String q, int page, int size) {
        Pageable pageable = PageableUtils.build(page, size, "createdAt:desc");
        Page<Question> questions;
        if (difficulty != null && !difficulty.isBlank()) {
            questions = questionRepository.findByTopicAndDifficulty(
                    topic != null ? topic : "", Difficulty.valueOf(difficulty.toUpperCase()), pageable);
        } else if (topic != null && !topic.isBlank()) {
            questions = questionRepository.findByTopic(topic, pageable);
        } else if (q != null && !q.isBlank()) {
            questions = questionRepository.search(q, pageable);
        } else {
            questions = questionRepository.findAll(pageable);
        }
        return PageResponse.from(questions, questions.stream().map(QuestionBankResponse::from).toList());
    }

    @Transactional(readOnly = true)
    public QuestionBankResponse get(Long id) {
        Question question = questionRepository.findById(id)
                .orElseThrow(() -> com.interviewai.exception.ResourceNotFoundException.of("Question", id));
        question.setViewsCount(question.getViewsCount() + 1);
        questionRepository.save(question);
        return QuestionBankResponse.from(question);
    }

    @Transactional
    public QuestionBankResponse create(CreateQuestionRequest request) {
        Question question = new Question();
        question.setTopic(request.topic());
        question.setSubTopic(request.subTopic());
        question.setQuestion(request.question());
        question.setAnswer(request.answer());
        question.setDifficulty(request.difficulty() != null ? request.difficulty() : Difficulty.MEDIUM);
        question.setType(request.type() != null ? request.type() : com.interviewai.common.enums.QuestionType.TEXT);
        question.setTags(request.tags());
        return QuestionBankResponse.from(questionRepository.save(question));
    }

    @Transactional
    public MessageResponse toggleBookmark(User user, Long questionId) {
        if (bookmarkService.isBookmarked(user, "QUESTION", questionId)) {
            return bookmarkService.remove(user, "QUESTION", questionId);
        }
        return bookmarkService.add(user, "QUESTION", questionId);
    }
}
