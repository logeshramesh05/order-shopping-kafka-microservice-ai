package com.example.ares.observation.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Persisted summary of an observation snapshot for incident history and reporting.
 */
@Entity
@Table(name = "ares_observation_snapshots")
public class ObservationMetricRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String snapshotId;

    @Column(nullable = false)
    private Instant observedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private HealthState overallState;

    private double cpuLoadPercent;

    private double memoryUsedPercent;

    private long freeDiskBytes;

    private int threadCount;

    @Lob
    @Column(nullable = false)
    private String componentSummaryJson;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSnapshotId() {
        return snapshotId;
    }

    public void setSnapshotId(String snapshotId) {
        this.snapshotId = snapshotId;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public void setObservedAt(Instant observedAt) {
        this.observedAt = observedAt;
    }

    public HealthState getOverallState() {
        return overallState;
    }

    public void setOverallState(HealthState overallState) {
        this.overallState = overallState;
    }

    public double getCpuLoadPercent() {
        return cpuLoadPercent;
    }

    public void setCpuLoadPercent(double cpuLoadPercent) {
        this.cpuLoadPercent = cpuLoadPercent;
    }

    public double getMemoryUsedPercent() {
        return memoryUsedPercent;
    }

    public void setMemoryUsedPercent(double memoryUsedPercent) {
        this.memoryUsedPercent = memoryUsedPercent;
    }

    public long getFreeDiskBytes() {
        return freeDiskBytes;
    }

    public void setFreeDiskBytes(long freeDiskBytes) {
        this.freeDiskBytes = freeDiskBytes;
    }

    public int getThreadCount() {
        return threadCount;
    }

    public void setThreadCount(int threadCount) {
        this.threadCount = threadCount;
    }

    public String getComponentSummaryJson() {
        return componentSummaryJson;
    }

    public void setComponentSummaryJson(String componentSummaryJson) {
        this.componentSummaryJson = componentSummaryJson;
    }
}
