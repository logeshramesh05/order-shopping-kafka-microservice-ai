package com.example.ares.observation.api;

import com.example.ares.observation.dto.ObservationHistoryResponse;
import com.example.ares.observation.dto.ObservationSnapshotResponse;
import com.example.ares.observation.service.ObservationService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Operations API for collecting and reading Observation Engine state.
 */
@Validated
@RestController
@RequestMapping("/api/operations/observations")
public class ObservationController {

    private final ObservationService observationService;

    public ObservationController(ObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping("/collect")
    public ResponseEntity<ObservationSnapshotResponse> collect() {
        return ResponseEntity.ok(ObservationSnapshotResponse.from(observationService.collectSnapshot()));
    }

    @GetMapping("/latest")
    public ResponseEntity<ObservationSnapshotResponse> latest() {
        return collect();
    }

    @GetMapping("/history")
    public ResponseEntity<List<ObservationHistoryResponse>> history(
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int limit) {
        List<ObservationHistoryResponse> history = observationService.recentHistory(limit).stream()
                .map(ObservationHistoryResponse::from)
                .toList();
        return ResponseEntity.ok(history);
    }
}
