package org.vitals.core.aggregator;

import java.util.Map;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.HealthCheck.HealthStatus;

public class WeightedScoringAggregator implements HealthResultAggregator {

    private static final Map<HealthStatus, Integer> STATUS_WEIGHTS = Map.of(
            HealthCheck.HealthStatus.CRITICAL, 100,
            HealthStatus.UNHEALTHY, 75,
            HealthStatus.DEGRADED, 50,
            HealthStatus.WARNING, 25,
            HealthStatus.HEALTHY, 0,
            HealthStatus.UNKNOWN, 10);

    private final String name;

    public WeightedScoringAggregator() {
        this.name = this.getClass().getSimpleName();
    }

    public WeightedScoringAggregator(String name) {
        this.name = name;
    }

    @Override
    public String getName() {
        return this.name;
    }

    @Override
    public HealthCheckResult aggregate(Map<HealthCheck, HealthCheckResult> results) {
        int totalScore = results.values().stream()
                .mapToInt(result -> STATUS_WEIGHTS.getOrDefault(result.getStatus(), 10))
                .sum();

        HealthStatus aggregatedStatus = deriveStatusFromScore(totalScore, results.size());
        return HealthCheckResult.builder()
                .status(aggregatedStatus)
                .message("Weighted aggregated health status")
                .build();
    }

    private HealthStatus deriveStatusFromScore(int totalScore, int totalChecks) {
        int averageScore = totalScore / totalChecks;
        if (averageScore >= 75)
            return HealthStatus.CRITICAL;
        if (averageScore >= 50)
            return HealthStatus.UNHEALTHY;
        if (averageScore >= 25)
            return HealthStatus.DEGRADED;
        return HealthStatus.HEALTHY;
    }

}
