package com.example.ares.observation.api;

import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.domain.ObservationMetricRecord;
import com.example.ares.observation.domain.ObservationSnapshot;
import com.example.ares.observation.domain.ResourceSample;
import com.example.ares.observation.port.ActuatorHealthPort;
import com.example.ares.observation.port.DockerObservationPort;
import com.example.ares.observation.port.KafkaObservationPort;
import com.example.ares.observation.port.ObservationHistoryPort;
import com.example.ares.observation.port.SystemMetricsPort;
import com.example.ares.observation.service.ObservationService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ObservationControllerTest {

    private final FakeObservationHistoryPort observationHistoryPort = new FakeObservationHistoryPort();
    private final ObservationService observationService = new ObservationService(
            new EmptyDockerObservationPort(),
            () -> new ObservedComponent("kafka", com.example.ares.observation.domain.ComponentType.KAFKA, HealthState.UP, "test", Instant.now(), java.util.Map.of()),
            new EmptyActuatorHealthPort(),
            () -> new ResourceSample(Instant.parse("2026-07-03T00:00:00Z"), 10.0, 20.0, 1000, 5),
            observationHistoryPort
    );
    private final MockMvc mockMvc = MockMvcBuilders
            .standaloneSetup(new ObservationController(observationService))
            .build();

    @Test
    void collectReturnsObservationSnapshot() throws Exception {
        observationHistoryPort.savedRecord = record();

        mockMvc.perform(post("/api/operations/observations/collect"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.overallState", is("UP")));
    }

    @Test
    void historyReturnsStoredSnapshots() throws Exception {
        observationHistoryPort.history = List.of(record());

        mockMvc.perform(get("/api/operations/observations/history").param("limit", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].snapshotId", is("snapshot-1")))
                .andExpect(jsonPath("$[0].overallState", is("UP")));
    }

    private ObservationMetricRecord record() {
        ObservationMetricRecord record = new ObservationMetricRecord();
        record.setSnapshotId("snapshot-1");
        record.setObservedAt(Instant.parse("2026-07-03T00:00:00Z"));
        record.setOverallState(HealthState.UP);
        record.setCpuLoadPercent(10.0);
        record.setMemoryUsedPercent(20.0);
        record.setFreeDiskBytes(1000);
        record.setThreadCount(5);
        record.setComponentSummaryJson("[]");
        return record;
    }

    private static class EmptyDockerObservationPort implements DockerObservationPort {
        @Override
        public List<ObservedComponent> observeContainers() {
            return List.of();
        }
    }

    private static class EmptyActuatorHealthPort implements ActuatorHealthPort {
        @Override
        public List<ObservedComponent> observeServiceHealth() {
            return List.of();
        }
    }

    private static class FakeObservationHistoryPort implements ObservationHistoryPort {
        private ObservationMetricRecord savedRecord;
        private List<ObservationMetricRecord> history = List.of();

        @Override
        public ObservationMetricRecord save(ObservationSnapshot snapshot) {
            return savedRecord == null ? new ObservationMetricRecord() : savedRecord;
        }

        @Override
        public List<ObservationMetricRecord> findRecent(int limit) {
            return history;
        }
    }
}
