package com.example.ares.observation.dto;

import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservationMetricRecord;

import java.time.Instant;

/**
 * Compact historical observation record for operations dashboards.
 */
public record ObservationHistoryResponse(
        String snapshotId,
        Instant observedAt,
        HealthState overallState,
        double cpuLoadPercent,
        double memoryUsedPercent,
        long freeDiskBytes,
        int threadCount,
        String componentSummaryJson
) {

    public static ObservationHistoryResponse from(ObservationMetricRecord record) {
        return new ObservationHistoryResponse(
                record.getSnapshotId(),
                record.getObservedAt(),
                record.getOverallState(),
                record.getCpuLoadPercent(),
                record.getMemoryUsedPercent(),
                record.getFreeDiskBytes(),
                record.getThreadCount(),
                record.getComponentSummaryJson()
        );
    }
}
