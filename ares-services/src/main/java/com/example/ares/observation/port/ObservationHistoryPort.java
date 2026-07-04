package com.example.ares.observation.port;

import com.example.ares.observation.domain.ObservationMetricRecord;
import com.example.ares.observation.domain.ObservationSnapshot;

import java.util.List;

/**
 * Port for storing and loading Observation Engine snapshot history.
 */
public interface ObservationHistoryPort {

    ObservationMetricRecord save(ObservationSnapshot snapshot);

    List<ObservationMetricRecord> findRecent(int limit);
}
