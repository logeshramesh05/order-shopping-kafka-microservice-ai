package com.example.ares.observation.domain;

import java.time.Instant;
import java.util.Map;

/**
 * Runtime health view for one component in the distributed platform.
 */
public record ObservedComponent(
        String name,
        ComponentType type,
        HealthState state,
        String detail,
        Instant observedAt,
        Map<String, String> attributes
) {
}
