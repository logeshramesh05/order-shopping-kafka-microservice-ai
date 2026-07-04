package com.example.ares.observation.adapter;

import com.example.ares.configuration.ObservationProperties;
import com.example.ares.observation.domain.ComponentType;
import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.port.KafkaObservationPort;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.AdminClientConfig;
import org.apache.kafka.common.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.TimeUnit;

/**
 * Kafka AdminClient adapter for cluster-level observation.
 */
@Component
public class KafkaAdminObservationAdapter implements KafkaObservationPort {

    private static final Logger log = LoggerFactory.getLogger(KafkaAdminObservationAdapter.class);

    private final ObservationProperties properties;

    public KafkaAdminObservationAdapter(ObservationProperties properties) {
        this.properties = properties;
    }

    @Override
    public ObservedComponent observeKafka() {
        Instant observedAt = Instant.now();
        Properties adminProperties = new Properties();
        adminProperties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, properties.getKafkaBootstrapServers());
        adminProperties.put(AdminClientConfig.REQUEST_TIMEOUT_MS_CONFIG, 2000);
        adminProperties.put(AdminClientConfig.DEFAULT_API_TIMEOUT_MS_CONFIG, 2500);

        try (AdminClient adminClient = AdminClient.create(adminProperties)) {
            Collection<Node> nodes = adminClient.describeCluster()
                    .nodes()
                    .get(3, TimeUnit.SECONDS);
            return new ObservedComponent(
                    "kafka",
                    ComponentType.KAFKA,
                    nodes.isEmpty() ? HealthState.DEGRADED : HealthState.UP,
                    "Kafka brokers discovered: " + nodes.size(),
                    observedAt,
                    Map.of(
                            "bootstrapServers", properties.getKafkaBootstrapServers(),
                            "brokerCount", String.valueOf(nodes.size())
                    )
            );
        } catch (Exception ex) {
            log.warn("Kafka observation failed", ex);
            return new ObservedComponent(
                    "kafka",
                    ComponentType.KAFKA,
                    HealthState.DOWN,
                    "Kafka unavailable: " + ex.getMessage(),
                    observedAt,
                    Map.of("bootstrapServers", properties.getKafkaBootstrapServers())
            );
        }
    }
}
