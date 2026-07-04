package com.example.ares.intelligence.adapter;

import com.example.ares.configuration.IntelligenceProperties;
import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.domain.AnalysisSeverity;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class GroqAdapterTest {

    @Test
    void analyzeReturnsUnavailableWhenApiKeyIsMissing() {
        IntelligenceProperties properties = properties("");
        GroqAdapter adapter = new GroqAdapter(properties, new RestTemplate());

        AIAnalysisResult result = adapter.analyze(request());

        assertThat(result.rootCause()).isEqualTo("AI provider unavailable");
        assertThat(result.recommendation()).contains("GROQ_API_KEY");
    }

    @Test
    void analyzeMapsStructuredGroqResponse() {
        IntelligenceProperties properties = properties("test-key");
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        GroqAdapter adapter = new GroqAdapter(properties, restTemplate);

        server.expect(requestTo("https://api.groq.test/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer test-key"))
                .andRespond(withSuccess("""
                        {
                          "choices": [{
                            "message": {
                              "content": "{\\"rootCause\\":\\"Kafka lag\\",\\"severity\\":\\"HIGH\\",\\"businessImpact\\":\\"Delayed shipping\\",\\"recommendation\\":\\"Scale consumers\\",\\"confidence\\":0.91,\\"summary\\":\\"Consumer lag is elevated\\"}"
                            }
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        AIAnalysisResult result = adapter.analyze(request());

        assertThat(result.rootCause()).isEqualTo("Kafka lag");
        assertThat(result.severity()).isEqualTo(AnalysisSeverity.HIGH);
        assertThat(result.recommendation()).isEqualTo("Scale consumers");
        assertThat(result.confidence()).isEqualTo(0.91);
        server.verify();
    }

    private IntelligenceProperties properties(String apiKey) {
        IntelligenceProperties properties = new IntelligenceProperties();
        properties.setGroqBaseUrl("https://api.groq.test/chat/completions");
        properties.setGroqModel("openai/gpt-oss-120b");
        properties.setGroqApiKey(apiKey);
        return properties;
    }

    private AIAnalysisRequest request() {
        return new AIAnalysisRequest(
                "incident-1",
                "Please briefly explain the importance of fast AI inference.",
                List.of("log line"),
                Map.of("latencyMs", "100"),
                Map.of("order-service", "UP"),
                Instant.now()
        );
    }
}
