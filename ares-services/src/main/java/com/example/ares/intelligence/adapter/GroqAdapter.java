package com.example.ares.intelligence.adapter;

import com.example.ares.configuration.IntelligenceProperties;
import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.domain.AnalysisSeverity;
import com.example.ares.intelligence.port.AIAnalysisPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.ObjectNode;

import java.time.Instant;
import java.util.Iterator;
import java.util.Map;

/**
 * Groq adapter for OpenAI-compatible chat completions.
 */
@Component
public class GroqAdapter implements AIAnalysisPort {

    private static final Logger log = LoggerFactory.getLogger(GroqAdapter.class);

    private final IntelligenceProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public GroqAdapter(IntelligenceProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public AIAnalysisResult analyze(AIAnalysisRequest request) {
        if (properties.getGroqApiKey() == null || properties.getGroqApiKey().isBlank()) {
            return unavailable(request, "GROQ_API_KEY is not configured.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(properties.getGroqApiKey());

            String response = restTemplate.postForObject(
                    properties.getGroqBaseUrl(),
                    new HttpEntity<>(buildPayload(request).toString(), headers),
                    String.class
            );
            String content = extractContent(response);
            return parseResult(request, content);
        } catch (Exception ex) {
            log.warn("Groq analysis request failed", ex);
            return unavailable(request, "Groq analysis failed: " + ex.getMessage());
        }
    }

    private ObjectNode buildPayload(AIAnalysisRequest request) {
        ObjectNode root = objectMapper.createObjectNode();
        root.put("model", properties.getGroqModel());

        ArrayNode messages = objectMapper.createArrayNode();
        ObjectNode message = objectMapper.createObjectNode();
        message.put("role", "user");
        message.put("content", buildPrompt(request));
        messages.add(message);
        root.set("messages", messages);
        root.put("temperature", 0.1);
        return root;
    }

    private String buildPrompt(AIAnalysisRequest request) {
        return """
                You are ARES, an autonomous resilience analysis engine.
                Analyze the following incident context and return JSON only with:
                rootCause, severity, businessImpact, recommendation, confidence, summary.

                Incident ID: %s
                Prompt: %s
                Logs:
                %s
                Metrics:
                %s
                Health:
                %s
                """.formatted(
                request.incidentId(),
                request.prompt(),
                String.join("\n", request.logs()),
                formatMap(request.metrics()),
                formatMap(request.health())
        );
    }

    private String formatMap(Map<String, String> values) {
        if (values.isEmpty()) {
            return "none";
        }
        StringBuilder builder = new StringBuilder();
        values.forEach((key, value) -> builder.append(key).append("=").append(value).append("\n"));
        return builder.toString().trim();
    }

    private String extractContent(String response) throws Exception {
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.get("choices");
        if (choices == null || !choices.isArray() || choices.isEmpty()) {
            return response == null ? "" : response;
        }
        JsonNode content = choices.get(0).get("message").get("content");
        return content == null ? "" : content.asText();
    }

    private AIAnalysisResult parseResult(AIAnalysisRequest request, String content) {
        try {
            JsonNode node = objectMapper.readTree(stripCodeFence(content));
            return new AIAnalysisResult(
                    request.incidentId(),
                    "groq",
                    properties.getGroqModel(),
                    text(node, "rootCause", "Unknown"),
                    severity(text(node, "severity", "UNKNOWN")),
                    text(node, "businessImpact", "Unknown"),
                    text(node, "recommendation", "No recommendation returned"),
                    number(node, "confidence", 0.0),
                    text(node, "summary", content),
                    Instant.now()
            );
        } catch (Exception ex) {
            return new AIAnalysisResult(
                    request.incidentId(),
                    "groq",
                    properties.getGroqModel(),
                    "Unstructured AI response",
                    AnalysisSeverity.UNKNOWN,
                    "Unknown",
                    "Review the summary and rerun with more structured incident context.",
                    0.0,
                    content,
                    Instant.now()
            );
        }
    }

    private String stripCodeFence(String content) {
        if (content == null) {
            return "";
        }
        String trimmed = content.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline >= 0 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }

    private String text(JsonNode node, String field, String fallback) {
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? fallback : value.asText();
    }

    private double number(JsonNode node, String field, double fallback) {
        JsonNode value = node.get(field);
        return value == null || !value.isNumber() ? fallback : value.asDouble();
    }

    private AnalysisSeverity severity(String value) {
        try {
            return AnalysisSeverity.valueOf(value.trim().toUpperCase());
        } catch (Exception ex) {
            return AnalysisSeverity.UNKNOWN;
        }
    }

    private AIAnalysisResult unavailable(AIAnalysisRequest request, String reason) {
        return new AIAnalysisResult(
                request.incidentId(),
                "groq",
                properties.getGroqModel(),
                "AI provider unavailable",
                AnalysisSeverity.UNKNOWN,
                "Unknown until AI analysis succeeds",
                reason,
                0.0,
                reason,
                Instant.now()
        );
    }
}
