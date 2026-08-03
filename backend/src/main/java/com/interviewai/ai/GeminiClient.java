package com.interviewai.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Thin, provider-agnostic wrapper around Google Gemini's REST API.
 *
 * <p>When no API key is configured, or a call fails, every method returns
 * {@link Optional#empty()} so callers can transparently fall back to
 * deterministic/heuristic behaviour. This keeps the whole platform usable
 * in development and CI without credentials.</p>
 */
@Slf4j
@Component
public class GeminiClient {

    private final String apiKey;
    private final String model;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public GeminiClient(
            @Value("${app.ai.gemini.api-key:}") String apiKey,
            @Value("${app.ai.gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            @Value("${app.ai.gemini.model:gemini-1.5-flash}") String model,
            @Value("${app.ai.gemini.timeout-seconds:60}") long timeoutSeconds,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.model = model;
        this.objectMapper = objectMapper;

        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(timeoutSeconds))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(timeoutSeconds));

        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .build();
    }

    /** Whether a real Gemini backend is available. */
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    /** Plain text completion. Empty when disabled or on failure. */
    public Optional<String> complete(String system, String user) {
        Optional<JsonNode> node = completeJson(system, user);
        return node.map(n -> n.isTextual() ? n.asText() : n.toString());
    }

    /** JSON completion. Empty when disabled or on failure. */
    public Optional<JsonNode> completeJson(String system, String user) {
        if (!enabled()) {
            log.warn("Gemini AI disabled: no API key configured");
            return Optional.empty();
        }
        try {
            Map<String, Object> body = buildRequest(system, user);
            String raw = restClient.post()
                    .uri("/v1beta/models/{model}:generateContent?key={key}", model, apiKey)
                    .body(body)
                    .retrieve()
                    .body(String.class);
            return extractText(raw);
        } catch (Exception e) {
            log.warn("Gemini request failed: {}", e.getMessage());
            return Optional.empty();
        }
    }

    private Map<String, Object> buildRequest(String system, String user) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("temperature", 0.4);
        generationConfig.put("maxOutputTokens", 4096);

        Map<String, Object> request = new LinkedHashMap<>();
        request.put("systemInstruction", Map.of("parts", List.of(Map.of("text", system))));
        request.put("contents", List.of(Map.of(
                "role", "user",
                "parts", List.of(Map.of("text", user)))));
        request.put("generationConfig", generationConfig);
        return request;
    }

    private Optional<JsonNode> extractText(String raw) throws Exception {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        JsonNode root = objectMapper.readTree(raw);
        JsonNode text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (text.isMissingNode() || text.asText().isBlank()) {
            log.warn("Gemini returned no usable text: {}", truncate(raw));
            return Optional.empty();
        }
        String content = stripFences(text.asText());
        return Optional.of(objectMapper.readTree(content));
    }

    private String stripFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstLine = trimmed.indexOf('\n');
            if (firstLine >= 0) {
                trimmed = trimmed.substring(firstLine + 1);
            }
            if (trimmed.endsWith("```")) {
                trimmed = trimmed.substring(0, trimmed.length() - 3);
            }
        }
        return trimmed.trim();
    }

    private String truncate(String value) {
        return value != null && value.length() > 300 ? value.substring(0, 300) + "…" : value;
    }
}
