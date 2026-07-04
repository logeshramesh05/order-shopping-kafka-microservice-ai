package com.example.ares.observation.adapter;

import com.example.ares.observation.domain.ResourceSample;
import com.example.ares.observation.port.SystemMetricsPort;
import org.springframework.stereotype.Component;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadMXBean;
import java.time.Instant;

/**
 * JVM and host metric adapter backed by standard management beans.
 */
@Component
public class JvmSystemMetricsAdapter implements SystemMetricsPort {

    @Override
    public ResourceSample sample() {
        com.sun.management.OperatingSystemMXBean osBean =
                (com.sun.management.OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();

        double cpuLoad = normalizePercent(osBean.getCpuLoad());
        long totalMemory = osBean.getTotalMemorySize();
        long freeMemory = osBean.getFreeMemorySize();
        double memoryUsed = totalMemory <= 0 ? 0.0 : ((double) (totalMemory - freeMemory) / totalMemory) * 100.0;
        long freeDiskBytes = new File(".").getFreeSpace();

        return new ResourceSample(
                Instant.now(),
                cpuLoad,
                memoryUsed,
                freeDiskBytes,
                threadBean.getThreadCount()
        );
    }

    private double normalizePercent(double load) {
        if (load < 0) {
            return 0.0;
        }
        return Math.min(100.0, load * 100.0);
    }
}
