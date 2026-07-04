package com.example.ares.observation.port;

import com.example.ares.observation.domain.ObservedComponent;

import java.util.List;

/**
 * Port for observing Spring Boot Actuator health from platform services.
 */
public interface ActuatorHealthPort {

    List<ObservedComponent> observeServiceHealth();
}
