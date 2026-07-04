package com.example.ares.observation.integration;

import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservationMetricRecord;
import com.example.ares.observation.domain.ObservationSnapshot;
import com.example.ares.observation.domain.ResourceSample;
import com.example.ares.observation.port.ObservationHistoryPort;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "ares.observation.docker-enabled=false",
        "ares.observation.service-health-endpoints="
})
class ObservationPersistenceIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8")
            .withDatabaseName("ares")
            .withUsername("ares")
            .withPassword("ares");

    @Autowired
    private ObservationHistoryPort observationHistoryPort;

    @DynamicPropertySource
    static void mysqlProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.driver-class-name", MYSQL::getDriverClassName);
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
    }

    @Test
    void saveAndReadRecentObservationSnapshot() {
        ObservationSnapshot snapshot = new ObservationSnapshot(
                "integration-snapshot-1",
                Instant.now(),
                HealthState.UP,
                new ResourceSample(Instant.now(), 1.0, 2.0, 3, 4),
                List.of()
        );

        ObservationMetricRecord saved = observationHistoryPort.save(snapshot);
        List<ObservationMetricRecord> recent = observationHistoryPort.findRecent(10);

        assertThat(saved.getId()).isNotNull();
        assertThat(recent).extracting(ObservationMetricRecord::getSnapshotId)
                .contains("integration-snapshot-1");
    }
}
