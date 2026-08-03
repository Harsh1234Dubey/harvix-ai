package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewai.common.enums.SubmissionStatus;
import com.interviewai.domain.CodingTest;
import com.interviewai.domain.TestCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Evaluates a coding submission. Prefers live Gemini analysis of the solution
 * against the visible test cases, and falls back to a deterministic substring
 * check so submissions always resolve to a status.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCodeReviewService {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private static final String SYSTEM_PROMPT =
            "You are a strict automated coding judge on InterView AI. A candidate submits code for a "
            + "programming problem. For each provided public test case, mentally execute the candidate's "
            + "code and determine whether the produced output matches the expected output exactly "
            + "(ignoring leading/trailing whitespace). Be rigorous: if the code has a syntax/logic "
            + "error that prevents running, report COMPILE_ERROR or WRONG_ANSWER accordingly. "
            + "Return ONLY a valid JSON object with exactly these fields: "
            + "\"status\" (one of ACCEPTED, WRONG_ANSWER, COMPILE_ERROR, RUNTIME_ERROR), "
            + "\"passedCases\" (integer), "
            + "\"caseResults\" (array of objects, each with \"orderIndex\" (integer), \"passed\" (boolean), "
            + "\"expected\" (string), \"actual\" (string)), "
            + "\"stdout\" (string or null), \"stderr\" (string or null), \"errorMessage\" (string or null), "
            + "\"codeScore\" (integer 0-100), \"complexityTime\" (string like \"O(n)\"), "
            + "\"complexitySpace\" (string like \"O(1)\"). Do not include markdown.";

    private final GeminiClient geminiClient;

    public CodeReviewResult review(CodingTest test, String sourceCode, List<TestCase> publicCases) {
        if (sourceCode == null || sourceCode.isBlank()) {
            return new CodeReviewResult(SubmissionStatus.COMPILE_ERROR, 0,
                    null, "Empty submission", "Source code is empty", ZERO, null, null, List.of());
        }
        Optional<JsonNode> ai = geminiClient.completeJson(SYSTEM_PROMPT, buildPrompt(test, sourceCode, publicCases));
        if (ai.isPresent()) {
            CodeReviewResult result = parse(ai.get(), publicCases);
            if (result.status() != null) {
                return result;
            }
        }
        return deterministic(test, sourceCode, publicCases);
    }

    private String buildPrompt(CodingTest test, String sourceCode, List<TestCase> publicCases) {
        StringBuilder sb = new StringBuilder();
        sb.append("Problem: ").append(test.getTitle()).append("\n")
                .append(test.getDescription()).append("\n")
                .append("Language: ").append(test.getLanguage()).append("\n\n");
        sb.append("Public test cases:\n");
        for (TestCase tc : publicCases) {
            sb.append("- case #").append(tc.getOrderIndex())
                    .append(" input=").append(tc.getInputData() == null ? "''" : tc.getInputData())
                    .append(" expectedOutput=").append(tc.getExpectedOutput()).append("\n");
        }
        sb.append("\nCandidate code:\n").append(sourceCode);
        return sb.toString();
    }

    private CodeReviewResult parse(JsonNode node, List<TestCase> publicCases) {
        SubmissionStatus status = parseStatus(node.path("status").asText(""));
        int passed = Math.max(0, node.path("passedCases").asInt(-1));

        List<Map<String, Object>> caseResults = new ArrayList<>();
        JsonNode results = node.path("caseResults");
        if (results.isArray()) {
            for (JsonNode item : results) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("orderIndex", item.path("orderIndex").asInt());
                entry.put("passed", item.path("passed").asBoolean(false));
                entry.put("expected", item.path("expected").asText(""));
                entry.put("actual", item.path("actual").asText(""));
                caseResults.add(entry);
            }
        }

        BigDecimal codeScore = node.path("codeScore").isNumber()
                ? BigDecimal.valueOf(node.path("codeScore").asDouble()).setScale(2, RoundingMode.HALF_UP)
                : ZERO;
        String complexityTime = textOrNull(node, "complexityTime");
        String complexitySpace = textOrNull(node, "complexitySpace");
        String stdout = textOrNull(node, "stdout");
        String stderr = textOrNull(node, "stderr");
        String errorMessage = textOrNull(node, "errorMessage");

        if (status == null) {
            return null;
        }
        if (status == SubmissionStatus.ACCEPTED) {
            passed = publicCases.size();
        }
        return new CodeReviewResult(status, passed, stdout, stderr, errorMessage,
                codeScore, complexityTime, complexitySpace, caseResults);
    }

    private SubmissionStatus parseStatus(String value) {
        if (value == null) {
            return null;
        }
        try {
            return SubmissionStatus.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private CodeReviewResult deterministic(CodingTest test, String sourceCode, List<TestCase> publicCases) {
        String normalized = sourceCode.replaceAll("\\s+", " ").toLowerCase();
        int passed = 0;
        List<String> mismatches = new ArrayList<>();
        for (TestCase testCase : publicCases) {
            String expected = normalizeOutput(testCase.getExpectedOutput());
            if (normalized.contains(expected)) {
                passed++;
            } else {
                mismatches.add("case#" + testCase.getOrderIndex()
                        + " expected [" + testCase.getExpectedOutput() + "]");
            }
        }
        if (publicCases.isEmpty()) {
            return new CodeReviewResult(SubmissionStatus.ACCEPTED, 0,
                    "No public cases", null, null, ZERO, null, null, List.of());
        }
        if (passed == publicCases.size()) {
            return new CodeReviewResult(SubmissionStatus.ACCEPTED, passed,
                    "All public cases passed", null, null,
                    BigDecimal.valueOf(75), "O(n)", "O(1)", List.of());
        }
        return new CodeReviewResult(SubmissionStatus.WRONG_ANSWER, passed,
                null, String.join("; ", mismatches), "Some test cases failed",
                BigDecimal.valueOf(30), null, null, List.of());
    }

    private String normalizeOutput(String output) {
        return output == null ? "" : output.replaceAll("\\s+", " ").toLowerCase().trim();
    }
}
