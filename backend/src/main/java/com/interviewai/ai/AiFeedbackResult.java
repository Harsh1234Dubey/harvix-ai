package com.interviewai.ai;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public record AiFeedbackResult(
        BigDecimal overall,
        BigDecimal communication,
        BigDecimal confidence,
        BigDecimal technical,
        BigDecimal grammar,
        BigDecimal fluency,
        BigDecimal keywordMatch,
        BigDecimal speakingSpeed,
        List<String> strengths,
        List<String> weaknesses,
        String suggestions,
        String recommendation,
        List<Map<String, Object>> detailed
) {
}
