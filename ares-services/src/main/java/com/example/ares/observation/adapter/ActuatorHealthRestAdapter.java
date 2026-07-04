package com.example.ares.observation.adapter;

import com.example.ares.configuration.ObservationProperties;
import com.example.ares.observation.domain.ComponentType;
import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.port.ActuatorHealthPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * HTTP adapter for collecting Spring Boot Actuator health from platform services.
 */
@Component
public class ActuatorHealthRestAdapter implements ActuatorHealthPort {

    private static final Logger log = LoggerFactory.getLogger(ActuatorHealthRestAdapter.class);

    private final RestTemplate restTemplate;
    private final ObservationProperties properties;

    public ActuatorHealthRestAdapter(RestTemplate restTemplate, ObservationProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public List<ObservedComponent> observeServiceHealth() {
        List<ObservedComponent> components = new ArrayList<>();
        properties.getServiceHealthEndpoints().forEach((serviceName, healthUrl) ->
                components.add(observe(serviceName, healthUrl)));
        return components;
    }

    @SuppressWarnings("unchecked")
    private ObservedComponent observe(String serviceName, String healthUrl) {
        Instant observedAt = Instant.now();
        try {
            Map<String, Object> response = restTemplate.getForObject(healthUrl, Map.class);
            String status = response == null ? "UNKNOWN" : String.valueOf(response.getOrDefault("status", "UNKNOWN"));
            HealthState state = "UP".equalsIgnoreCase(status) ? HealthState.UP : HealthState.DEGRADED;
            return new ObservedComponent(
                    serviceName,
                    ComponentType.SERVICE,
                    state,
                    "Actuator health returned " + status,
                    observedAt,
                    Map.of("healthUrl", healthUrl, "rawStatus", status)
            );
        } catch (Exception ex) {
            log.warn("Actuator health check failed for {}", serviceName, ex);
            return new ObservedComponent(
                    serviceName,
                    ComponentType.SERVICE,
                    HealthState.DOWN,
                    "Actuator health unavailable: " + ex.getMessage(),
                    observedAt,
                    Map.of("healthUrl", healthUrl)
            );
        }
    }
}
