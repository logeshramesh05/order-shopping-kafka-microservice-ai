package com.example.ares.observation.service;

import com.example.ares.observation.domain.ComponentType;
import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.domain.ObservationSnapshot;
import com.example.ares.observation.domain.ResourceSample;
import com.example.ares.observation.port.ActuatorHealthPort;
import com.example.ares.observation.port.DockerObservationPort;
import com.example.ares.observation.port.KafkaObservationPort;
import com.example.ares.observation.port.ObservationHistoryPort;
import com.example.ares.observation.port.SystemMetricsPort;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ObservationServiceTest {

    private final FakeDockerObservationPort dockerObservationPort = new FakeDockerObservationPort();
    private final FakeKafkaObservationPort kafkaObservationPort = new FakeKafkaObservationPort();
    private final FakeActuatorHealthPort actuatorHealthPort = new FakeActuatorHealthPort();
    private final FakeSystemMetricsPort systemMetricsPort = new FakeSystemMetricsPort();
    private final FakeObservationHistoryPort observationHistoryPort = new FakeObservationHistoryPort();

    private final ObservationService observationService = new ObservationService(
            dockerObservationPort,
            kafkaObservationPort,
            actuatorHealthPort,
            systemMetricsPort,
            observationHistoryPort
    );

    @Test
    void collectSnapshotReturnsDownWhenAnyComponentIsDown() {
        systemMetricsPort.sample = new ResourceSample(Instant.now(), 12.5, 42.0, 1024, 8);
        dockerObservationPort.components = List.of(component("shipping-service", HealthState.DOWN));
        kafkaObservationPort.component = component("kafka", HealthState.UP);
        actuatorHealthPort.components = List.of(component("order-service", HealthState.UP));

        ObservationSnapshot snapshot = observationService.collectSnapshot();

        assertThat(snapshot.overallState()).isEqualTo(HealthState.DOWN);
        assertThat(snapshot.components()).hasSize(3);
        assertThat(observationHistoryPort.savedSnapshots).hasSize(1);
    }

    @Test
    void collectSnapshotReturnsDegradedWhenNoComponentIsDownButOneIsDegraded() {
        systemMetricsPort.sample = new ResourceSample(Instant.now(), 12.5, 42.0, 1024, 8);
        dockerObservationPort.components = List.of(component("shipping-service", HealthState.UP));
        kafkaObservationPort.component = component("kafka", HealthState.DEGRADED);
        actuatorHealthPort.components = List.of(component("order-service", HealthState.UP));

        ObservationSnapshot snapshot = observationService.collectSnapshot();

        assertThat(snapshot.overallState()).isEqualTo(HealthState.DEGRADED);
        assertThat(observationHistoryPort.savedSnapshots).hasSize(1);
    }

    private ObservedComponent component(String name, HealthState state) {
        return new ObservedComponent(name, ComponentType.SERVICE, state, "test", Instant.now(), Map.of());
    }

    private static class FakeDockerObservationPort implements DockerObservationPort {
        private List<ObservedComponent> components = List.of();

        @Override
        public List<ObservedComponent> observeContainers() {
            return components;
        }
    }

    private static class FakeKafkaObservationPort implements KafkaObservationPort {
        private ObservedComponent component;

        @Override
        public ObservedComponent observeKafka() {
            return component;
        }
    }

    private static class FakeActuatorHealthPort implements ActuatorHealthPort {
        private List<ObservedComponent> components = List.of();

        @Override
        public List<ObservedComponent> observeServiceHealth() {
            return components;
        }
    }

    private static class FakeSystemMetricsPort implements SystemMetricsPort {
        private ResourceSample sample;

        @Override
        public ResourceSample sample() {
            return sample;
        }
    }

    private static class FakeObservationHistoryPort implements ObservationHistoryPort {
        private final List<ObservationSnapshot> savedSnapshots = new ArrayList<>();

        @Override
        public com.example.ares.observation.domain.ObservationMetricRecord save(ObservationSnapshot snapshot) {
            savedSnapshots.add(snapshot);
            return new com.example.ares.observation.domain.ObservationMetricRecord();
        }

        @Override
        public List<com.example.ares.observation.domain.ObservationMetricRecord> findRecent(int limit) {
            return List.of();
        }
    }
}
