package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.domain.InterviewAnswer;
import com.interviewai.domain.InterviewQuestion;
import com.interviewai.domain.InterviewSession;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiFeedbackServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AiFeedbackService service;

    private InterviewSession session() {
        InterviewSession s = new InterviewSession();
        s.setSkill("Java");
        s.setDifficulty(Difficulty.MEDIUM);
        return s;
    }

    @Test
    void parsesGeminiFeedback() throws Exception {
        String json = "{\"overall\":8.4,\"communication\":8.0,\"confidence\":7.5,\"technical\":8.9,"
                + "\"grammar\":9.0,\"fluency\":8.0,\"keywordMatch\":7.0,\"speakingSpeed\":8.0,"
                + "\"strengths\":[\"Clear examples\"],\"weaknesses\":[\"Rushed ending\"],"
                + "\"suggestions\":\"Pause between points.\",\"hiringRecommendation\":\"ADVANCE\","
                + "\"detailed\":[{\"question\":\"Q1\",\"score\":8,\"comment\":\"Solid.\"}]}";
        when(geminiClient.completeJson(anyString(), anyString()))
                .thenReturn(Optional.of(mapper.readTree(json)));

        AiFeedbackResult result = service.generate(session(), List.of(), List.of());
        assertThat(result.overall()).isEqualByComparingTo("8.40");
        assertThat(result.technical()).isEqualByComparingTo("8.90");
        assertThat(result.strengths()).containsExactly("Clear examples");
        assertThat(result.recommendation()).isEqualTo("ADVANCE");
        assertThat(result.detailed()).hasSize(1);
    }

    @Test
    void fallsBackToHeuristicWhenAiDisabled() {
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());

        InterviewQuestion q = new InterviewQuestion();
        q.setId(1L);
        q.setQuestionText("Explain dependency injection.");
        q.setDifficulty(Difficulty.MEDIUM);

        InterviewAnswer a = new InterviewAnswer();
        a.setQuestion(q);
        a.setAnswerText("Dependency injection is passing dependencies into an object instead of creating them.");

        AiFeedbackResult result = service.generate(session(), List.of(q), List.of(a));
        assertThat(result.overall()).isNotNull();
        assertThat(result.overall()).isGreaterThan(BigDecimal.ZERO);
        assertThat(result.recommendation()).isIn(
                "STRONG_ADVANCE", "ADVANCE", "WEAK_ADVANCE", "HOLD", "NO_GO");
        assertThat(result.detailed()).hasSize(1);
        assertThat(result.strengths()).isNotEmpty();
    }

    @Test
    void emptySessionYieldsZeroScore() {
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());

        AiFeedbackResult result = service.generate(session(), List.of(), List.of());
        assertThat(result.overall()).isEqualByComparingTo("0.00");
        assertThat(result.recommendation()).isEqualTo("NO_GO");
    }
}
