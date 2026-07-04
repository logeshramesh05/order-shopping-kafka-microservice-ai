package com.example.ares.intelligence.port;

import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;

/**
 * Port for AI-assisted operational analysis.
 */
public interface AIAnalysisPort {

    AIAnalysisResult analyze(AIAnalysisRequest request);
}
