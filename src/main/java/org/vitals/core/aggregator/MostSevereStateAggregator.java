package org.vitals.core.aggregator;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.HealthCheck.HealthStatus;

public class MostSevereStateAggregator implements HealthResultAggregator {
        private static final List<HealthStatus> PRIORITY_ORDER = List.of(
                        HealthStatus.CRITICAL,
                        HealthStatus.UNHEALTHY,
                        HealthStatus.DEGRADED,
                        HealthStatus.WARNING,
                        HealthStatus.HEALTHY,
                        HealthStatus.UNKNOWN);

        private final String name;

        public MostSevereStateAggregator(String name) {
                this.name = name;
        }

        public MostSevereStateAggregator() {
                this.name = this.getClass().getSimpleName();
        }

        @Override
        public String getName() {
                return this.name;
        }

        @Override
        public HealthCheckResult aggregate(Map<HealthCheck, HealthCheckResult> results) {
                HealthStatus worstStatus = results.values().stream()
                                .map(HealthCheckResult::getStatus)
                                .min(Comparator.comparingInt(PRIORITY_ORDER::indexOf))
                                .orElse(HealthStatus.UNKNOWN);

                return HealthCheckResult.builder()
                                .status(worstStatus)
                                .message("Aggregated health status")
                                .build();
        }
}
