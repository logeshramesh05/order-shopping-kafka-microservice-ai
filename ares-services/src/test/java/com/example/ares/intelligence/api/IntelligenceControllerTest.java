package com.example.ares.intelligence.api;

import com.example.ares.intelligence.domain.AIAnalysisRequest;
import com.example.ares.intelligence.domain.AIAnalysisResult;
import com.example.ares.intelligence.domain.AnalysisSeverity;
import com.example.ares.intelligence.port.AIAnalysisPort;
import com.example.ares.intelligence.service.IntelligenceService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class IntelligenceControllerTest {

    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new IntelligenceController(new IntelligenceService(new FakeAnalysisPort())))
            .build();

    @Test
    void analyzeReturnsAiAnalysis() throws Exception {
        mockMvc.perform(post("/api/operations/intelligence/analyze")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "incidentId": "incident-1",
                                  "prompt": "Please briefly explain the importance of fast AI inference."
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.incidentId", is("incident-1")))
                .andExpect(jsonPath("$.provider", is("fake")))
                .andExpect(jsonPath("$.severity", is("LOW")));
    }

    private static class FakeAnalysisPort implements AIAnalysisPort {
        @Override
        public AIAnalysisResult analyze(AIAnalysisRequest request) {
            return new AIAnalysisResult(
                    request.incidentId(),
                    "fake",
                    "fake-model",
                    "root cause",
                    AnalysisSeverity.LOW,
                    "business impact",
                    "recommendation",
                    0.8,
                    "summary",
                    Instant.now()
            );
        }
    }
}
