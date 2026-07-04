package com.example.ares.intelligence.service;

import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.dto.AIAnalysisRequestDto;
import com.example.ares.intelligence.port.AIAnalysisPort;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Application service for provider-neutral AI analysis use cases.
 */
@Service
public class IntelligenceService {

    private final AIAnalysisPort analysisPort;

    public IntelligenceService(AIAnalysisPort analysisPort) {
        this.analysisPort = analysisPort;
    }

    public AIAnalysisResult analyze(AIAnalysisRequestDto requestDto) {
        AIAnalysisRequest request = new AIAnalysisRequest(
                normalizeIncidentId(requestDto.incidentId()),
                requestDto.prompt(),
                requestDto.logs() == null ? List.of() : List.copyOf(requestDto.logs()),
                requestDto.metrics() == null ? Map.of() : Map.copyOf(requestDto.metrics()),
                requestDto.health() == null ? Map.of() : Map.copyOf(requestDto.health()),
                Instant.now()
        );
        return analysisPort.analyze(request);
    }

    private String normalizeIncidentId(String incidentId) {
        if (incidentId == null || incidentId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return incidentId.trim();
    }
}
