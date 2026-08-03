package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewai.domain.InterviewAnswer;
import com.interviewai.domain.InterviewQuestion;
import com.interviewai.domain.InterviewSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Generates structured feedback for a completed AI mock interview. Prefers live
 * Gemini output and falls back to a deterministic length/coverage heuristic so
 * candidates always receive a report.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiFeedbackService {

    private static final String SYSTEM_PROMPT =
            "You are a senior interview coach at InterView AI. You evaluate a candidate's answers in a "
            + "mock interview and produce detailed, constructive feedback. Score each dimension on a "
            + "scale of 0 to 10 (decimals allowed). Be fair and specific. "
            + "Return ONLY a valid JSON object with exactly these fields: "
            + "\"overall\" (number 0-10), \"communication\" (number), \"confidence\" (number), "
            + "\"technical\" (number), \"grammar\" (number), \"fluency\" (number), "
            + "\"keywordMatch\" (number), \"speakingSpeed\" (number), "
            + "\"strengths\" (array of strings), \"weaknesses\" (array of strings), "
            + "\"suggestions\" (string), \"hiringRecommendation\" (one of STRONG_ADVANCE, ADVANCE, "
            + "WEAK_ADVANCE, HOLD, NO_GO), and \"detailed\" (array of objects, one per question, each "
            + "with \"question\" (string), \"score\" (number) and \"comment\" (string)). "
            + "Do not include markdown or surrounding text.";

    private final GeminiClient geminiClient;

    public AiFeedbackResult generate(InterviewSession session,
                                     List<InterviewQuestion> questions,
                                     List<InterviewAnswer> answers) {
        Optional<JsonNode> ai = geminiClient.completeJson(SYSTEM_PROMPT, buildPrompt(session, questions, answers));
        if (ai.isPresent()) {
            AiFeedbackResult result = parse(ai.get());
            if (result.overall() != null) {
                return result;
            }
        }
        return heuristic(questions, answers);
    }

    private String buildPrompt(InterviewSession session, List<InterviewQuestion> questions,
                               List<InterviewAnswer> answers) {
        Map<Long, String> byQuestion = new HashMap<>();
        for (InterviewAnswer answer : answers) {
            byQuestion.put(answer.getQuestion().getId(), answer.getAnswerText());
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Mock interview: skill=").append(session.getSkill())
                .append(", difficulty=").append(session.getDifficulty()).append("\n\n");
        if (questions.isEmpty()) {
            sb.append("The candidate did not answer any questions.");
        }
        int index = 0;
        for (InterviewQuestion q : questions) {
            index++;
            sb.append("Q").append(index).append(" [").append(q.getDifficulty()).append("] ")
                    .append(q.getQuestionText()).append("\n");
            String answer = byQuestion.get(q.getId());
            if (answer == null || answer.isBlank()) {
                sb.append("A").append(index).append(": [skipped / no answer]\n");
            } else {
                sb.append("A").append(index).append(": ").append(answer).append("\n");
            }
        }
        return sb.toString();
    }

    private AiFeedbackResult parse(JsonNode node) {
        List<String> strengths = textList(node, "strengths");
        List<String> weaknesses = textList(node, "weaknesses");
        List<Map<String, Object>> detailed = new ArrayList<>();
        JsonNode detailedNode = node.path("detailed");
        if (detailedNode.isArray()) {
            for (JsonNode item : detailedNode) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("question", item.path("question").asText(""));
                entry.put("score", item.path("score").isNumber() ? item.path("score").asDouble() : null);
                entry.put("comment", item.path("comment").asText(""));
                detailed.add(entry);
            }
        }
        return new AiFeedbackResult(
                number(node, "overall"),
                number(node, "communication"),
                number(node, "confidence"),
                number(node, "technical"),
                number(node, "grammar"),
                number(node, "fluency"),
                number(node, "keywordMatch"),
                number(node, "speakingSpeed"),
                strengths,
                weaknesses,
                node.path("suggestions").asText(null),
                node.path("hiringRecommendation").asText(null),
                detailed);
    }

    private List<String> textList(JsonNode node, String field) {
        List<String> list = new ArrayList<>();
        JsonNode array = node.path(field);
        if (array.isArray()) {
            for (JsonNode item : array) {
                String text = item.asText(null);
                if (text != null && !text.isBlank()) {
                    list.add(text.trim());
                }
            }
        }
        return list;
    }

    private BigDecimal number(JsonNode node, String field) {
        JsonNode value = node.path(field);
        if (!value.isNumber()) {
            return null;
        }
        return BigDecimal.valueOf(value.asDouble()).setScale(2, RoundingMode.HALF_UP);
    }

    private AiFeedbackResult heuristic(List<InterviewQuestion> questions, List<InterviewAnswer> answers) {
        Map<Long, InterviewAnswer> byQuestion = new HashMap<>();
        for (InterviewAnswer answer : answers) {
            byQuestion.put(answer.getQuestion().getId(), answer);
        }
        int answered = 0;
        int totalWords = 0;
        List<Map<String, Object>> detailed = new ArrayList<>();
        for (InterviewQuestion q : questions) {
            InterviewAnswer answer = byQuestion.get(q.getId());
            String text = answer != null ? answer.getAnswerText() : null;
            boolean has = text != null && !text.isBlank();
            if (has) {
                answered++;
            }
            int words = has ? text.trim().split("\\s+").length : 0;
            totalWords += words;
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("question", q.getQuestionText());
            entry.put("score", has ? Math.min(10, 4 + (words / 12)) : 0);
            entry.put("comment", has ? "Good attempt; aim for a clearer structure next time."
                    : "Question was skipped.");
            detailed.add(entry);
        }
        int total = Math.max(1, questions.size());
        double coverage = (double) answered / total;
        int avgWords = total / Math.max(1, answered);

        BigDecimal overall = scale(coverage * 10);
        BigDecimal communication = scale(Math.min(10, coverage * 10 + (avgWords >= 30 ? 1 : 0)));
        BigDecimal confidence = scale(Math.min(10, coverage * 10 + (avgWords >= 20 ? 0.5 : 0)));
        BigDecimal technical = scale(coverage * 10);
        BigDecimal grammar = scale(Math.min(10, 7 + (avgWords >= 40 ? 1 : 0)));
        BigDecimal fluency = scale(coverage * 10);
        BigDecimal keywordMatch = scale(coverage * 10);
        BigDecimal speakingSpeed = scale(7.5);

        List<String> strengths = new ArrayList<>();
        List<String> weaknesses = new ArrayList<>();
        if (coverage >= 0.8) {
            strengths.add("Answered most questions without skipping.");
        } else {
            weaknesses.add("Several questions were left unanswered.");
        }
        if (avgWords >= 40) {
            strengths.add("Answers were detailed and well developed.");
        } else {
            weaknesses.add("Answers were brief; add concrete examples and structure.");
        }
        if (coverage == 0) {
            strengths.add("The session was completed — a good first step.");
        }

        String recommendation;
        double score = coverage * 10;
        if (score >= 8) {
            recommendation = "STRONG_ADVANCE";
        } else if (score >= 6) {
            recommendation = "ADVANCE";
        } else if (score >= 4) {
            recommendation = "WEAK_ADVANCE";
        } else if (score >= 2) {
            recommendation = "HOLD";
        } else {
            recommendation = "NO_GO";
        }

        return new AiFeedbackResult(
                overall, communication, confidence, technical, grammar, fluency,
                keywordMatch, speakingSpeed, strengths, weaknesses,
                "Practice structuring answers with situation-task-action-result and expand your examples.",
                recommendation, detailed);
    }

    private BigDecimal scale(double value) {
        return BigDecimal.valueOf(value).setScale(2, RoundingMode.HALF_UP);
    }
}
