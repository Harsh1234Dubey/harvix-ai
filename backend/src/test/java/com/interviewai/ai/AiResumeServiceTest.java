package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.interviewai.domain.Job;
import com.interviewai.exception.BadRequestException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AiResumeServiceTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Mock
    private GeminiClient geminiClient;

    @InjectMocks
    private AiResumeService service;

    private Job job() {
        Job job = new Job();
        job.setId(1L);
        job.setTitle("Senior Backend Engineer");
        job.setDescription("Design and build scalable backend services with Java and Spring Boot, using PostgreSQL.");
        job.setRequirements("5+ years Java, Spring Boot, PostgreSQL, REST APIs, Docker, microservices.");
        job.setResponsibilities("Build and ship backend services end to end.");
        return job;
    }

    @Test
    void blankResumeIsRejected() {
        assertThatThrownBy(() -> service.analyze("   ", job()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void fallsBackToKeywordMatchWhenAiDisabled() {
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.empty());

        AiResumeService.AtsResult result = service.analyze(
                "Senior Backend Engineer with Java, Spring Boot, PostgreSQL, REST APIs, Docker and microservices experience.",
                job());

        assertThat(result.source()).isEqualTo("KEYWORD_FALLBACK");
        assertThat(result.score()).isGreaterThan(java.math.BigDecimal.ZERO);
        assertThat(result.matchedKeywords()).contains("java", "spring", "boot");
        assertThat(result.summary()).contains("Keyword match");
    }

    @Test
    void usesGeminiResultWhenAvailable() throws Exception {
        String json = "{\"score\":82,\"summary\":\"Strong match.\","
                + "\"strengths\":[\"Good stack alignment\"],\"gaps\":[\"Missing SaaS experience\"],"
                + "\"matchedKeywords\":[\"Java\",\"Spring Boot\"],\"missingKeywords\":[\"SaaS\",\"Kafka\"]}";
        JsonNode node = mapper.readTree(json);
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.of(node));

        AiResumeService.AtsResult result = service.analyze("Java developer with Spring Boot.", job());

        assertThat(result.source()).isEqualTo("AI");
        assertThat(result.score()).isEqualByComparingTo("82.00");
        assertThat(result.strengths()).containsExactly("Good stack alignment");
        assertThat(result.gaps()).containsExactly("Missing SaaS experience");
        assertThat(result.matchedKeywords()).containsExactly("Java", "Spring Boot");
        assertThat(result.missingKeywords()).containsExactly("SaaS", "Kafka");
    }

    @Test
    void invalidAiJsonFallsBackToKeywords() throws Exception {
        JsonNode node = mapper.readTree("{\"score\":\"not-a-number\"}");
        when(geminiClient.completeJson(anyString(), anyString())).thenReturn(Optional.of(node));

        AiResumeService.AtsResult result = service.analyze("Java Spring Boot engineer.", job());

        assertThat(result.source()).isEqualTo("KEYWORD_FALLBACK");
        assertThat(result.matchedKeywords()).contains("java");
    }
}
