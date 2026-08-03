package com.interviewai.dto.request;

import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
        @NotNull Long questionId,
        String answerText,
        String voiceToText,
        Boolean skipped
) {
}
