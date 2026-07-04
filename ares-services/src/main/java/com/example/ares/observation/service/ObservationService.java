package com.example.ares.observation.service;

import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.domain.ObservationMetricRecord;
import com.example.ares.observation.domain.ObservationSnapshot;
import com.example.ares.observation.domain.ResourceSample;
import com.example.ares.observation.port.ActuatorHealthPort;
import com.example.ares.observation.port.DockerObservationPort;
import com.example.ares.observation.port.KafkaObservationPort;
import com.example.ares.observation.port.ObservationHistoryPort;
import com.example.ares.observation.port.SystemMetricsPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Core Observation Engine use case. It coordinates ports and persists snapshots.
 */
@Service
public class ObservationService {

    private static final Logger log = LoggerFactory.getLogger(ObservationService.class);

    private final DockerObservationPort dockerObservationPort;
    private final KafkaObservationPort kafkaObservationPort;
    private final ActuatorHealthPort actuatorHealthPort;
    private final SystemMetricsPort systemMetricsPort;
    private final ObservationHistoryPort observationHistoryPort;

    public ObservationService(DockerObservationPort dockerObservationPort,
                              KafkaObservationPort kafkaObservationPort,
                              ActuatorHealthPort actuatorHealthPort,
                              SystemMetricsPort systemMetricsPort,
                              ObservationHistoryPort observationHistoryPort) {
        this.dockerObservationPort = dockerObservationPort;
        this.kafkaObservationPort = kafkaObservationPort;
        this.actuatorHealthPort = actuatorHealthPort;
        this.systemMetricsPort = systemMetricsPort;
        this.observationHistoryPort = observationHistoryPort;
    }

    public ObservationSnapshot collectSnapshot() {
        ResourceSample resourceSample = systemMetricsPort.sample();
        List<ObservedComponent> components = new ArrayList<>();
        components.addAll(dockerObservationPort.observeContainers());
        components.add(kafkaObservationPort.observeKafka());
        components.addAll(actuatorHealthPort.observeServiceHealth());
        components.sort(Comparator.comparing(ObservedComponent::type).thenComparing(ObservedComponent::name));

        ObservationSnapshot snapshot = new ObservationSnapshot(
                UUID.randomUUID().toString(),
                Instant.now(),
                calculateOverallState(components),
                resourceSample,
                List.copyOf(components)
        );
        observationHistoryPort.save(snapshot);
        return snapshot;
    }

    public List<ObservationMetricRecord> recentHistory(int limit) {
        return observationHistoryPort.findRecent(limit);
    }

    @Scheduled(fixedDelayString = "${ares.observation.collection-interval-ms:30000}")
    void collectScheduledSnapshot() {
        try {
            ObservationSnapshot snapshot = collectSnapshot();
            log.info("Observation snapshot {} collected with state {}", snapshot.snapshotId(), snapshot.overallState());
        } catch (Exception ex) {
            log.error("Scheduled observation collection failed", ex);
        }
    }

    private HealthState calculateOverallState(List<ObservedComponent> components) {
        if (components.stream().anyMatch(component -> component.state() == HealthState.DOWN)) {
            return HealthState.DOWN;
        }
        if (components.stream().anyMatch(component -> component.state() == HealthState.DEGRADED)) {
            return HealthState.DEGRADED;
        }
        if (components.stream().allMatch(component -> component.state() == HealthState.UNKNOWN)) {
            return HealthState.UNKNOWN;
        }
        return HealthState.UP;
    }
}
