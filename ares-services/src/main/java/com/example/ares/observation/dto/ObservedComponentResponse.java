package com.example.ares.observation.dto;

import com.example.ares.observation.domain.ComponentType;
import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;

import java.time.Instant;
import java.util.Map;

/**
 * API representation of one observed platform component.
 */
public record ObservedComponentResponse(
        String name,
        ComponentType type,
        HealthState state,
        String detail,
        Instant observedAt,
        Map<String, String> attributes
) {

    public static ObservedComponentResponse from(ObservedComponent component) {
        return new ObservedComponentResponse(
                component.name(),
                component.type(),
                component.state(),
                component.detail(),
                component.observedAt(),
                component.attributes()
        );
    }
}
