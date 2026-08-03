package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.domain.Question;
import com.interviewai.repository.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

/**
 * Generates interview questions for a mock session. Prefers live Gemini output
 * and falls back to the seeded question bank, then to generic templates.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiQuestionService {

    private static final int MAX_QUESTIONS = 8;

    private static final String SYSTEM_PROMPT =
            "You are an expert technical interviewer for a SaaS product called InterView AI. "
            + "Given a skill and a difficulty level, generate interview questions a candidate would "
            + "face in a real interview for that skill. Mix conceptual questions, practical scenarios "
            + "and behavioural questions. Every question must be self-contained and answerable aloud "
            + "in 2-4 minutes. "
            + "Return ONLY a valid JSON array of objects, each with exactly these fields: "
            + "\"question\" (string), \"topic\" (string), \"category\" (one of TECHNICAL, BEHAVIORAL, "
            + "SYSTEM_DESIGN, CODING, SITUATIONAL) and \"difficulty\" (one of EASY, MEDIUM, HARD). "
            + "Do not include any markdown, explanations or surrounding text.";

    private static final List<String> TEMPLATES = List.of(
            "Walk me through your background and how it relates to this role.",
            "Tell me about a challenging technical problem you solved and how you approached it.",
            "How do you prioritise your work when you have multiple deadlines?",
            "Describe a time you received difficult feedback and how you handled it.",
            "Why are you interested in this role, and what makes you a good fit?");

    private final GeminiClient geminiClient;
    private final QuestionRepository questionRepository;

    public List<GeneratedQuestion> generate(String skill, Difficulty difficulty, int count) {
        int target = Math.min(Math.max(count, 1), MAX_QUESTIONS);
        Optional<JsonNode> ai = geminiClient.completeJson(SYSTEM_PROMPT, buildPrompt(skill, difficulty, target));
        if (ai.isPresent()) {
            List<GeneratedQuestion> generated = parse(ai.get());
            if (!generated.isEmpty()) {
                return generated.size() > target ? generated.subList(0, target) : generated;
            }
        }
        return fallback(skill, difficulty, target);
    }

    private String buildPrompt(String skill, Difficulty difficulty, int count) {
        return "Generate exactly " + count + " interview questions for the skill \"" + skill
                + "\" at difficulty " + difficulty + ".";
    }

    private List<GeneratedQuestion> parse(JsonNode node) {
        List<GeneratedQuestion> questions = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return questions;
        }
        Iterator<JsonNode> elements = node.elements();
        while (elements.hasNext()) {
            JsonNode item = elements.next();
            String text = item.path("question").asText(null);
            if (text == null || text.isBlank()) {
                continue;
            }
            questions.add(new GeneratedQuestion(
                    text.trim(),
                    item.path("topic").asText("General"),
                    item.path("category").asText("TECHNICAL"),
                    item.path("difficulty").asText("MEDIUM")));
        }
        return questions;
    }

    private List<GeneratedQuestion> fallback(String skill, Difficulty difficulty, int count) {
        List<GeneratedQuestion> result = new ArrayList<>();
        Page<Question> matched = questionRepository.findByTopicAndDifficulty(
                skill, difficulty, PageRequest.of(0, count));
        for (Question q : matched.getContent()) {
            result.add(new GeneratedQuestion(q.getQuestion(), q.getTopic(),
                    q.getType() != null ? q.getType().name() : "TECHNICAL", q.getDifficulty().name()));
        }
        if (result.size() < count) {
            Page<Question> topicAny = questionRepository.findByTopic(skill,
                    PageRequest.of(0, count - result.size()));
            for (Question q : topicAny.getContent()) {
                result.add(new GeneratedQuestion(q.getQuestion(), q.getTopic(),
                        q.getType() != null ? q.getType().name() : "TECHNICAL", q.getDifficulty().name()));
            }
        }
        int i = 0;
        while (result.size() < count) {
            result.add(new GeneratedQuestion(TEMPLATES.get(i % TEMPLATES.size()), skill,
                    "BEHAVIORAL", difficulty.name()));
            i++;
        }
        return result;
    }
}
