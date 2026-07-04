package com.example.ares.observation.port;

import com.example.ares.observation.domain.ObservedComponent;

import java.util.List;

/**
 * Port for observing Docker container runtime status.
 */
public interface DockerObservationPort {

    List<ObservedComponent> observeContainers();
}
