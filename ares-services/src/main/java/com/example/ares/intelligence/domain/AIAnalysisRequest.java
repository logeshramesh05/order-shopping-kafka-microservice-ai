package com.example.ares.intelligence.domain;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Provider-neutral request for AI-assisted operational analysis.
 */
public record AIAnalysisRequest(
        String incidentId,
        String prompt,
        List<String> logs,
        Map<String, String> metrics,
        Map<String, String> health,
        Instant requestedAt
) {
}
