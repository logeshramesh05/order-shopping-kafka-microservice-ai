package com.example.ares.intelligence.dto;

import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.domain.AnalysisSeverity;

import java.time.Instant;

/**
 * REST response body for AI analysis.
 */
public record AIAnalysisResponseDto(
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

    public static AIAnalysisResponseDto from(AIAnalysisResult result) {
        return new AIAnalysisResponseDto(
                result.incidentId(),
                result.provider(),
                result.model(),
                result.rootCause(),
                result.severity(),
                result.businessImpact(),
                result.recommendation(),
                result.confidence(),
                result.summary(),
                result.analyzedAt()
        );
    }
}
