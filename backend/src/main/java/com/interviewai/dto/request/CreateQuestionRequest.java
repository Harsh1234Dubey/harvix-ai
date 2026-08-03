package com.interviewai.dto.request;

import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.QuestionType;
import jakarta.validation.constraints.NotBlank;

public record CreateQuestionRequest(
        @NotBlank String topic,
        String subTopic,
        @NotBlank String question,
        String answer,
        Difficulty difficulty,
        QuestionType type,
        String tags
) {
}
