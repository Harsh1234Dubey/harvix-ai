package com.interviewai.dto.response;

import com.interviewai.domain.InterviewQuestion;

public record QuestionResponse(
        Long id,
        Long sessionId,
        String questionText,
        String topic,
        String difficulty,
        String category,
        boolean followUp,
        int orderIndex
) {
    public static QuestionResponse from(InterviewQuestion q) {
        return new QuestionResponse(
                q.getId(),
                q.getSession().getId(),
                q.getQuestionText(),
                q.getTopic(),
                q.getDifficulty() != null ? q.getDifficulty().name() : null,
                q.getCategory(),
                q.isFollowUp(),
                q.getOrderIndex());
    }
}
