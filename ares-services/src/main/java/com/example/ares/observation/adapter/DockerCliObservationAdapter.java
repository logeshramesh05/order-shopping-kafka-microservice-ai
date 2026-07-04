package com.example.ares.observation.adapter;

import com.example.ares.configuration.ObservationProperties;
import com.example.ares.observation.domain.ComponentType;
import com.example.ares.observation.domain.HealthState;
import com.example.ares.observation.domain.ObservedComponent;
import com.example.ares.observation.port.DockerObservationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Docker CLI adapter. Business logic sees only normalized observed components.
 */
@Component
public class DockerCliObservationAdapter implements DockerObservationPort {

    private static final Logger log = LoggerFactory.getLogger(DockerCliObservationAdapter.class);

    private final ObservationProperties properties;

    public DockerCliObservationAdapter(ObservationProperties properties) {
        this.properties = properties;
    }

    @Override
    public List<ObservedComponent> observeContainers() {
        if (!properties.isDockerEnabled()) {
            return List.of(new ObservedComponent(
                    "docker",
                    ComponentType.DOCKER,
                    HealthState.UNKNOWN,
                    "Docker observation disabled",
                    Instant.now(),
                    Map.of()
            ));
        }

        try {
            Process process = new ProcessBuilder(
                    "docker",
                    "ps",
                    "--all",
                    "--format",
                    "{{.Names}}|{{.Status}}|{{.Image}}"
            ).redirectErrorStream(true).start();

            List<String> lines = readLines(process);
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                return unavailable("Docker ps failed: " + String.join("\n", lines));
            }

            List<ObservedComponent> components = new ArrayList<>();
            for (String line : lines) {
                String[] parts = line.split("\\|", 3);
                if (parts.length < 2) {
                    continue;
                }
                String name = parts[0];
                String status = parts[1];
                String image = parts.length == 3 ? parts[2] : "unknown";
                components.add(new ObservedComponent(
                        name,
                        ComponentType.DOCKER,
                        status.toLowerCase().contains("up") ? HealthState.UP : HealthState.DOWN,
                        status,
                        Instant.now(),
                        Map.of("image", image)
                ));
            }
            return components;
        } catch (Exception ex) {
            log.warn("Docker observation failed", ex);
            return unavailable("Docker CLI unavailable: " + ex.getMessage());
        }
    }

    private List<ObservedComponent> unavailable(String detail) {
        return List.of(new ObservedComponent(
                "docker",
                ComponentType.DOCKER,
                HealthState.UNKNOWN,
                detail,
                Instant.now(),
                Map.of()
        ));
    }

    private List<String> readLines(Process process) throws Exception {
        List<String> lines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
        }
        return lines;
    }
}
