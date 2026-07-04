package com.example.ares.observation.dto;

import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservationSnapshot;

import java.time.Instant;
import java.util.List;

/**
 * API representation of an Observation Engine snapshot.
 */
public record ObservationSnapshotResponse(
        String snapshotId,
        Instant observedAt,
        HealthState overallState,
        ResourceSampleResponse resources,
        List<ObservedComponentResponse> components
) {

    public static ObservationSnapshotResponse from(ObservationSnapshot snapshot) {
        return new ObservationSnapshotResponse(
                snapshot.snapshotId(),
                snapshot.observedAt(),
                snapshot.overallState(),
                ResourceSampleResponse.from(snapshot.resources()),
                snapshot.components().stream()
                        .map(ObservedComponentResponse::from)
                        .toList()
        );
    }
}
