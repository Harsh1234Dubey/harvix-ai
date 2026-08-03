package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.common.enums.SubmissionStatus;
import com.interviewai.domain.CodingTest;
import com.interviewai.domain.TestCase;
import com.interviewai.domain.User;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiCodeReviewServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AiCodeReviewService service;

    private CodingTest test() {
        CodingTest t = new CodingTest();
        t.setId(1L);
        t.setTitle("Two Sum");
        t.setDescription("Find two numbers that sum to target");
        t.setLanguage("JAVA");
        t.setDifficulty(Difficulty.EASY);
        return t;
    }

    private TestCase case_() {
        TestCase tc = new TestCase();
        tc.setOrderIndex(0);
        tc.setInputData("1 2 3,5");
        tc.setExpectedOutput("1 2");
        return tc;
    }

    @Test
    void blankSourceIsCompileError() {
        CodeReviewResult result = service.review(test(), "   ", List.of(case_()));
        assertThat(result.status()).isEqualTo(SubmissionStatus.COMPILE_ERROR);
        assertThat(result.passed()).isZero();
    }

    @Test
    void fallsBackToDeterministicCheckWhenAiDisabled() {
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());

        CodeReviewResult accepted = service.review(test(), "return \"1 2\";", List.of(case_()));
        assertThat(accepted.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(accepted.passed()).isEqualTo(1);

        CodeReviewResult wrong = service.review(test(), "return \"9 9\";", List.of(case_()));
        assertThat(wrong.status()).isEqualTo(SubmissionStatus.WRONG_ANSWER);
        assertThat(wrong.passed()).isZero();
    }

    @Test
    void usesGeminiResultWhenAvailable() throws Exception {
        String json = "{\"status\":\"ACCEPTED\",\"passedCases\":2,"
                + "\"caseResults\":[{\"orderIndex\":0,\"passed\":true,\"expected\":\"1 2\",\"actual\":\"1 2\"}],"
                + "\"stdout\":\"1 2\",\"stderr\":null,\"errorMessage\":null,"
                + "\"codeScore\":92,\"complexityTime\":\"O(n)\",\"complexitySpace\":\"O(1)\"}";
        JsonNode node = mapper.readTree(json);
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.of(node));

        CodeReviewResult result = service.review(test(), "solution()", List.of(case_()));
        assertThat(result.status()).isEqualTo(SubmissionStatus.ACCEPTED);
        assertThat(result.codeScore()).isEqualByComparingTo("92.00");
        assertThat(result.complexityTime()).isEqualTo("O(n)");
        assertThat(result.complexitySpace()).isEqualTo("O(1)");
        assertThat(result.passed()).isEqualTo(1);
    }
}
