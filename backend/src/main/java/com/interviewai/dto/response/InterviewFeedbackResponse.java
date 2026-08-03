package com.interviewai.dto.response;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.domain.InterviewFeedback;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public record InterviewFeedbackResponse(
        Long id,
        Long sessionId,
        BigDecimal overallScore,
        BigDecimal communicationScore,
        BigDecimal confidenceScore,
        BigDecimal technicalScore,
        BigDecimal grammarScore,
        BigDecimal fluencyScore,
        BigDecimal keywordMatchScore,
        BigDecimal speakingSpeedScore,
        List<String> strengths,
        List<String> weaknesses,
        String suggestions,
        String hiringRecommendation,
        List<Map<String, Object>> detailed
) {
    private static final ObjectMapper MAPPER = new ObjectMapper();

    public static InterviewFeedbackResponse from(InterviewFeedback f) {
        return new InterviewFeedbackResponse(
                f.getId(),
                f.getSession().getId(),
                f.getOverallScore(),
                f.getCommunication(),
                f.getConfidence(),
                f.getTechnicalKnowledge(),
                f.getGrammar(),
                f.getFluency(),
                f.getKeywordMatch(),
                f.getSpeakingSpeed(),
                parseList(f.getStrengthsJson()),
                parseList(f.getWeaknessesJson()),
                f.getLearningSuggestions(),
                f.getHiringRecommendation(),
                parseDetailed(f.getDetailedJson()));
    }

    private static List<String> parseList(String json) {
        List<String> values = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        try {
            var node = MAPPER.readTree(json);
            if (node.isArray()) {
                node.forEach(item -> values.add(item.asText()));
            } else if (node.isTextual()) {
                values.add(node.asText());
            }
        } catch (Exception ignored) {
        }
        return values;
    }

    private static List<Map<String, Object>> parseDetailed(String json) {
        List<Map<String, Object>> values = new ArrayList<>();
        if (json == null || json.isBlank()) {
            return values;
        }
        try {
            var node = MAPPER.readTree(json);
            if (node.isArray()) {
                node.forEach(item -> {
                    Map<String, Object> entry = new LinkedHashMap<>();
                    if (item.isObject()) {
                        item.fields().forEachRemaining(f -> {
                            var v = f.getValue();
                            if (v.isNumber()) {
                                entry.put(f.getKey(), v.asDouble());
                            } else if (v.isBoolean()) {
                                entry.put(f.getKey(), v.asBoolean());
                            } else {
                                entry.put(f.getKey(), v.asText());
                            }
                        });
                    }
                    values.add(entry);
                });
            }
        } catch (Exception ignored) {
        }
        return values;
    }
}
