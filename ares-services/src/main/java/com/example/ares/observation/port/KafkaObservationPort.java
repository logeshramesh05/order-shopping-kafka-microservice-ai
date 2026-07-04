package com.example.ares.observation.port;

import com.example.ares.observation.domain.ObservedComponent;

/**
 * Port for observing Kafka cluster connectivity and metadata.
 */
public interface KafkaObservationPort {

    ObservedComponent observeKafka();
}
