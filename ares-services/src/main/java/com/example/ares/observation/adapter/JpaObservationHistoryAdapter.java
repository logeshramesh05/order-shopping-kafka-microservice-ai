package com.example.ares.observation.adapter;

import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.domain.ObservationMetricRecord;
import com.example.ares.observation.domain.ObservationSnapshot;
import com.example.ares.observation.port.ObservationHistoryPort;
import tools.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * JPA adapter that stores observation snapshots without leaking persistence APIs into services.
 */
@Repository
public class JpaObservationHistoryAdapter implements ObservationHistoryPort {

    private static final Logger log = LoggerFactory.getLogger(JpaObservationHistoryAdapter.class);

    private final ObservationMetricJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaObservationHistoryAdapter(ObservationMetricJpaRepository repository) {
        this.repository = repository;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ObservationMetricRecord save(ObservationSnapshot snapshot) {
        ObservationMetricRecord record = new ObservationMetricRecord();
        record.setSnapshotId(snapshot.snapshotId());
        record.setObservedAt(snapshot.observedAt());
        record.setOverallState(snapshot.overallState());
        record.setCpuLoadPercent(snapshot.resources().cpuLoadPercent());
        record.setMemoryUsedPercent(snapshot.resources().memoryUsedPercent());
        record.setFreeDiskBytes(snapshot.resources().freeDiskBytes());
        record.setThreadCount(snapshot.resources().threadCount());
        record.setComponentSummaryJson(toJson(snapshot.components()));
        return repository.save(record);
    }

    @Override
    public List<ObservationMetricRecord> findRecent(int limit) {
        return repository.findAllByOrderByObservedAtDesc(PageRequest.of(0, Math.max(1, limit)));
    }

    private String toJson(List<ObservedComponent> components) {
        try {
            return objectMapper.writeValueAsString(components);
        } catch (Exception ex) {
            log.warn("Failed to serialize observed components", ex);
            return "[]";
        }
    }
}
