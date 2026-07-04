package com.example.ares.intelligence.service;

import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.domain.AnalysisSeverity;
import com.example.ares.intelligence.dto.AIAnalysisRequestDto;
import com.example.ares.intelligence.port.AIAnalysisPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class IntelligenceServiceTest {

    private final CapturingAnalysisPort analysisPort = new CapturingAnalysisPort();
    private final IntelligenceService intelligenceService = new IntelligenceService(analysisPort);

    @Test
    void analyzeNormalizesEmptyCollectionsAndGeneratesIncidentId() {
        AIAnalysisResult result = intelligenceService.analyze(new AIAnalysisRequestDto(
                null,
                "Analyze fast AI inference",
                null,
                null,
                null
        ));

        assertThat(result.incidentId()).isNotBlank();
        assertThat(analysisPort.capturedRequest.logs()).isEmpty();
        assertThat(analysisPort.capturedRequest.metrics()).isEmpty();
        assertThat(analysisPort.capturedRequest.health()).isEmpty();
    }

    @Test
    void analyzePreservesIncidentContext() {
        intelligenceService.analyze(new AIAnalysisRequestDto(
                "incident-1",
                "Analyze shipping outage",
                List.of("shipping-service timeout"),
                Map.of("latencyMs", "1200"),
                Map.of("shipping-service", "DOWN")
        ));

        assertThat(analysisPort.capturedRequest.incidentId()).isEqualTo("incident-1");
        assertThat(analysisPort.capturedRequest.logs()).containsExactly("shipping-service timeout");
        assertThat(analysisPort.capturedRequest.metrics()).containsEntry("latencyMs", "1200");
        assertThat(analysisPort.capturedRequest.health()).containsEntry("shipping-service", "DOWN");
    }

    private static class CapturingAnalysisPort implements AIAnalysisPort {
        private AIAnalysisRequest capturedRequest;

        @Override
        public AIAnalysisResult analyze(AIAnalysisRequest request) {
            capturedRequest = request;
            return new AIAnalysisResult(
                    request.incidentId(),
                    "fake",
                    "fake-model",
                    "root cause",
                    AnalysisSeverity.LOW,
                    "none",
                    "none",
                    0.5,
                    "summary",
                    Instant.now()
            );
        }
    }
}
