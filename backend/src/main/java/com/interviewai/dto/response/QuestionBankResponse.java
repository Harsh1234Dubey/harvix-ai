package com.interviewai.dto.response;

import com.interviewai.domain.Question;

public record QuestionBankResponse(
        Long id,
        String topic,
        String subTopic,
        String question,
        String answer,
        String difficulty,
        String type,
        String tags,
        int viewsCount
) {
    public static QuestionBankResponse from(Question q) {
        return new QuestionBankResponse(
                q.getId(),
                q.getTopic(),
                q.getSubTopic(),
                q.getQuestion(),
                q.getAnswer(),
                q.getDifficulty() != null ? q.getDifficulty().name() : null,
                q.getType() != null ? q.getType().name() : null,
                q.getTags(),
                q.getViewsCount());
    }
}
