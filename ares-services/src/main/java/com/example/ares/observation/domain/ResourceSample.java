package com.example.ares.observation.domain;

import java.time.Instant;

/**
 * Host-level resource measurements captured by the Observation Engine.
 */
public record ResourceSample(
        Instant observedAt,
        double cpuLoadPercent,
        double memoryUsedPercent,
        long freeDiskBytes,
        int threadCount
) {
}
