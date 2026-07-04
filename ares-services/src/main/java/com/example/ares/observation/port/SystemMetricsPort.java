package com.example.ares.observation.port;

import com.example.ares.observation.domain.ResourceSample;

/**
 * Port for host/JVM resource observation.
 */
public interface SystemMetricsPort {

    ResourceSample sample();
}
