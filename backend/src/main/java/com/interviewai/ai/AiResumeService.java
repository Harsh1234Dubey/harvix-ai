package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewai.domain.Job;
import com.interviewai.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;

/**
 * Scores a resume against a job using an ATS-style analysis. Prefers a live
 * Gemini evaluation and falls back to a deterministic keyword match so a score
 * is always produced without credentials.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiResumeService {

    private static final BigDecimal MAX_SCORE = BigDecimal.valueOf(100);
    private static final Set<String> STOPWORDS = Set.of(
            "the", "and", "for", "with", "that", "this", "from", "you", "your", "are", "have", "has",
            "will", "can", "all", "any", "our", "their", "into", "over", "under", "about", "after",
            "also", "such", "which", "what", "when", "where", "who", "whom", "how", "not", "but",
            "work", "working", "experience", "ability", "skills", "skill", "knowledge", "required");

    private static final String SYSTEM_PROMPT =
            "You are an expert ATS (Applicant Tracking System) resume analyzer on Harvix AI. "
            + "A candidate's resume text and a job posting are provided. Evaluate the resume exactly as "
            + "a strict ATS would: presence and placement of keywords from the job description, "
            + "relevance of the candidate's experience and skills, formatting and parseability, and "
            + "quantifiable impact. Return ONLY a valid JSON object with exactly these fields: "
            + "\"score\" (integer 0-100), \"summary\" (2-3 sentence string), "
            + "\"strengths\" (array of strings), \"gaps\" (array of strings), "
            + "\"matchedKeywords\" (array of strings), \"missingKeywords\" (array of strings). "
            + "Do not include markdown.";

    private final GeminiClient geminiClient;

    public AtsResult analyze(String resumeText, Job job) {
        if (resumeText == null || resumeText.isBlank()) {
            throw new BadRequestException("Resume text is empty; could not be parsed for ATS analysis");
        }
        String jobText = jobText(job);
        Optional<JsonNode> ai = geminiClient.completeJson(SYSTEM_PROMPT,
                "Resume:\n" + resumeText + "\n\nJob title: " + job.getTitle()
                        + "\nJob posting:\n" + jobText);
        if (ai.isPresent()) {
            AtsResult parsed = parse(ai.get());
            if (parsed != null) {
                return parsed;
            }
        }
        return keywordFallback(resumeText, job);
    }

    private AtsResult parse(JsonNode node) {
        JsonNode scoreNode = node.path("score");
        if (!scoreNode.isNumber()) {
            return null;
        }
        int score = Math.max(0, Math.min(100, scoreNode.asInt()));
        return new AtsResult(
                BigDecimal.valueOf(score).setScale(2, RoundingMode.HALF_UP),
                textOrNull(node, "summary"),
                strings(node, "strengths"),
                strings(node, "gaps"),
                strings(node, "matchedKeywords"),
                strings(node, "missingKeywords"),
                "AI");
    }

    private AtsResult keywordFallback(String resumeText, Job job) {
        Set<String> resumeWords = normalize(resumeText);
        List<String> keywords = extractKeywords(jobText(job));

        List<String> matched = new ArrayList<>();
        List<String> missing = new ArrayList<>();
        for (String keyword : keywords) {
            if (resumeWords.contains(keyword)) {
                matched.add(keyword);
            } else if (missing.size() < 8) {
                missing.add(keyword);
            }
        }

        BigDecimal score = keywords.isEmpty()
                ? MAX_SCORE
                : BigDecimal.valueOf(matched.size())
                        .multiply(MAX_SCORE)
                        .divide(BigDecimal.valueOf(keywords.size()), 2, RoundingMode.HALF_UP);

        String summary = "Keyword match of " + matched.size() + "/" + keywords.size()
                + " against the job posting"
                + (matched.isEmpty() ? "" : ", including " + String.join(", ", matched.subList(0, Math.min(3, matched.size()))))
                + ".";

        List<String> strengths = matched.size() > 4
                ? List.of("Covers " + matched.size() + " of " + keywords.size() + " key terms from the job posting",
                "Good alignment with the listed skills and requirements")
                : List.of("Matches " + matched.size() + " of " + keywords.size() + " key terms from the job posting");

        List<String> gaps = missing.isEmpty()
                ? List.of("No obvious keyword gaps detected")
                : List.of("Missing keywords: " + String.join(", ", missing));

        return new AtsResult(score, summary, strengths, gaps, matched, missing, "KEYWORD_FALLBACK");
    }

    private String jobText(Job job) {
        StringBuilder sb = new StringBuilder(job.getTitle());
        if (job.getDescription() != null) {
            sb.append("\n").append(job.getDescription());
        }
        if (job.getRequirements() != null) {
            sb.append("\nRequirements: ").append(job.getRequirements());
        }
        if (job.getResponsibilities() != null) {
            sb.append("\nResponsibilities: ").append(job.getResponsibilities());
        }
        return sb.toString();
    }

    private List<String> extractKeywords(String text) {
        Set<String> words = normalize(text);
        List<String> keywords = new ArrayList<>();
        for (String word : words) {
            if (word.length() >= 3 && !STOPWORDS.contains(word)) {
                keywords.add(word);
            }
        }
        return keywords.stream().limit(30).toList();
    }

    private Set<String> normalize(String text) {
        Set<String> words = new LinkedHashSet<>();
        for (String part : text.toLowerCase(Locale.ROOT).split("[^a-z0-9+#.]+")) {
            String word = part.trim();
            if (!word.isBlank()) {
                words.add(word);
            }
        }
        return words;
    }

    private String textOrNull(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() && !value.asText().isBlank() ? value.asText().trim() : null;
    }

    private List<String> strings(JsonNode node, String field) {
        List<String> result = new ArrayList<>();
        JsonNode array = node.path(field);
        if (array.isArray()) {
            for (JsonNode item : array) {
                if (item.isTextual() && !item.asText().isBlank()) {
                    result.add(item.asText().trim());
                }
            }
        }
        return result;
    }

    public record AtsResult(BigDecimal score, String summary, List<String> strengths,
                            List<String> gaps, List<String> matchedKeywords, List<String> missingKeywords,
                            String source) {
    }
}
