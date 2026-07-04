package com.example.ares.configuration;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Externalized configuration for Observation Engine collection targets and timings.
 */
@Validated
@ConfigurationProperties(prefix = "ares.observation")
public class ObservationProperties {

    @Min(1000)
    private long collectionIntervalMs = 30_000;

    @NotBlank
    private String kafkaBootstrapServers = "localhost:9092";

    private boolean dockerEnabled = true;

    @Min(250)
    private int actuatorTimeoutMs = 2000;

    private Map<String, String> serviceHealthEndpoints = new LinkedHashMap<>();

    public long getCollectionIntervalMs() {
        return collectionIntervalMs;
    }

    public void setCollectionIntervalMs(long collectionIntervalMs) {
        this.collectionIntervalMs = collectionIntervalMs;
    }

    public String getKafkaBootstrapServers() {
        return kafkaBootstrapServers;
    }

    public void setKafkaBootstrapServers(String kafkaBootstrapServers) {
        this.kafkaBootstrapServers = kafkaBootstrapServers;
    }

    public boolean isDockerEnabled() {
        return dockerEnabled;
    }

    public void setDockerEnabled(boolean dockerEnabled) {
        this.dockerEnabled = dockerEnabled;
    }

    public int getActuatorTimeoutMs() {
        return actuatorTimeoutMs;
    }

    public void setActuatorTimeoutMs(int actuatorTimeoutMs) {
        this.actuatorTimeoutMs = actuatorTimeoutMs;
    }

    public Map<String, String> getServiceHealthEndpoints() {
        return serviceHealthEndpoints;
    }

    public void setServiceHealthEndpoints(Map<String, String> serviceHealthEndpoints) {
        this.serviceHealthEndpoints = serviceHealthEndpoints;
    }
}
