package com.example.ares.observation.dto;

import com.example.ares.observation.domain.ResourceSample;

import java.time.Instant;

/**
 * API representation of host/JVM resource metrics.
 */
public record ResourceSampleResponse(
        Instant observedAt,
        double cpuLoadPercent,
        double memoryUsedPercent,
        long freeDiskBytes,
        int threadCount
) {

    public static ResourceSampleResponse from(ResourceSample sample) {
        return new ResourceSampleResponse(
                sample.observedAt(),
                sample.cpuLoadPercent(),
                sample.memoryUsedPercent(),
                sample.freeDiskBytes(),
                sample.threadCount()
        );
    }
}
