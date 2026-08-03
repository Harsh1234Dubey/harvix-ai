package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.common.enums.Difficulty;
import com.interviewai.domain.Question;
import com.interviewai.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiQuestionServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private GeminiClient geminiClient;
    @Mock
    private QuestionRepository questionRepository;

    @InjectMocks
    private AiQuestionService service;

    @Test
    void parsesGeminiQuestions() throws Exception {
        String json = "[{\"question\":\"Explain REST vs GraphQL.\",\"topic\":\"Web\","
                + "\"category\":\"TECHNICAL\",\"difficulty\":\"MEDIUM\"}]";
        when(geminiClient.completeJson(anyString(), anyString()))
                .thenReturn(Optional.of(mapper.readTree(json)));

        List<GeneratedQuestion> questions = service.generate("Java", Difficulty.MEDIUM, 3);
        assertThat(questions).hasSize(1);
        assertThat(questions.get(0).question()).isEqualTo("Explain REST vs GraphQL.");
        assertThat(questions.get(0).category()).isEqualTo("TECHNICAL");
    }

    @Test
    void fallsBackToQuestionBankThenTemplates() {
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());
        when(questionRepository.findByTopicAndDifficulty(anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));
        when(questionRepository.findByTopic(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<GeneratedQuestion> questions = service.generate("Java", Difficulty.MEDIUM, 5);
        assertThat(questions).hasSize(5);
        assertThat(questions.get(0).question()).isNotBlank();
    }

    @Test
    void usesQuestionBankWhenGeminiDisabled() {
        Question q = new Question();
        q.setQuestion("What is a HashMap?");
        q.setTopic("Data Structures");
        q.setDifficulty(Difficulty.MEDIUM);

        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());
        when(questionRepository.findByTopicAndDifficulty(anyString(), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(q)));
        when(questionRepository.findByTopic(anyString(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        List<GeneratedQuestion> questions = service.generate("Data Structures", Difficulty.MEDIUM, 3);
        assertThat(questions).hasSize(3);
        assertThat(questions.get(0).question()).isEqualTo("What is a HashMap?");
    }
}
