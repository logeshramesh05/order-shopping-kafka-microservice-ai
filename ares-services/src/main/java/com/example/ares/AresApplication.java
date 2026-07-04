package com.example.ares;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the Autonomous Resilience Engine System runtime control plane.
 */
@SpringBootApplication
@EnableScheduling
@ConfigurationPropertiesScan
public class AresApplication {

    public static void main(String[] args) {
        SpringApplication.run(AresApplication.class, args);
    }
}
