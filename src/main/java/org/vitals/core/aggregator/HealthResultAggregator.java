package org.vitals.core.aggregator;

import org.vitals.core.HealthCheck;

import java.util.Map;

public interface HealthResultAggregator {
    HealthCheck.HealthCheckResult aggregate(Map<HealthCheck, HealthCheck.HealthCheckResult> results);

    String getName();
}
