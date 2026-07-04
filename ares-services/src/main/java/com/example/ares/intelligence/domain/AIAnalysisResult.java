package com.example.ares.intelligence.domain;

import java.time.Instant;

/**
 * Provider-neutral result returned by the Intelligence Engine.
 */
public record AIAnalysisResult(
        String incidentId,
        String provider,
        String model,
        String rootCause,
        AnalysisSeverity severity,
        String businessImpact,
        String recommendation,
        double confidence,
        String summary,
        Instant analyzedAt
) {
}
