package com.example.ares.intelligence.api;

import com.example.ares.intelligence.dto.AIAnalysisRequestDto;
import com.example.ares.intelligence.dto.AIAnalysisResponseDto;
import com.example.ares.intelligence.service.IntelligenceService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Operations API for AI-assisted incident analysis.
 */
@RestController
@RequestMapping("/api/operations/intelligence")
public class IntelligenceController {

    private final IntelligenceService intelligenceService;

    public IntelligenceController(IntelligenceService intelligenceService) {
        this.intelligenceService = intelligenceService;
    }

    @PostMapping("/analyze")
    public ResponseEntity<AIAnalysisResponseDto> analyze(@Valid @RequestBody AIAnalysisRequestDto request) {
        return ResponseEntity.ok(AIAnalysisResponseDto.from(intelligenceService.analyze(request)));
    }
}
