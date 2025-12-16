package com.autoclicker.core.model;

import java.io.Serializable;

/**
 * Represents a performance metric data point for click performance tracking.
 */
public class PerformanceMetric implements Serializable {
    private static final long serialVersionUID = 1L;

    private final long timestamp;
    private final double targetCps;
    private final double actualCps;
    private final long cpuUsage; // in percentage * 100 (e.g., 1500 = 15%)

    /**
     * Creates a new performance metric.
     *
     * @param timestamp The timestamp when the metric was recorded
     * @param targetCps The target clicks per second
     * @param actualCps The actual clicks per second achieved
     * @param cpuUsage The CPU usage (percentage * 100)
     */
    public PerformanceMetric(long timestamp, double targetCps, double actualCps, long cpuUsage) {
        this.timestamp = timestamp;
        this.targetCps = targetCps;
        this.actualCps = actualCps;
        this.cpuUsage = cpuUsage;
    }

    public long getTimestamp() { return timestamp; }
    public double getTargetCps() { return targetCps; }
    public double getActualCps() { return actualCps; }
    public long getCpuUsage() { return cpuUsage; }

    @Override
    public String toString() {
        return "PerformanceMetric{" +
                "timestamp=" + timestamp +
                ", targetCps=" + targetCps +
                ", actualCps=" + actualCps +
                ", cpuUsage=" + cpuUsage +
                '}';
    }
}