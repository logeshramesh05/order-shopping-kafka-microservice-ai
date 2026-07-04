package com.example.ares.intelligence.dto;

import jakarta.validation.constraints.NotBlank;

import java.util.List;
import java.util.Map;

/**
 * REST request body for AI analysis.
 */
public record AIAnalysisRequestDto(
        String incidentId,
        @NotBlank String prompt,
        List<String> logs,
        Map<String, String> metrics,
        Map<String, String> health
) {
}
