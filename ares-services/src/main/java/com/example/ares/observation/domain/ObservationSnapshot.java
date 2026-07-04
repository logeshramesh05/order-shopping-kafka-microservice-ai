package com.example.ares.observation.domain;

import java.time.Instant;
import java.util.List;

/**
 * Point-in-time operational snapshot across services, infrastructure, and host resources.
 */
public record ObservationSnapshot(
        String snapshotId,
        Instant observedAt,
        HealthState overallState,
        ResourceSample resources,
        List<ObservedComponent> components
) {
}
