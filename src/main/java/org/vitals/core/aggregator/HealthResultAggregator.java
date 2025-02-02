package org.vitals.core.aggregator;

import java.util.Map;

import org.vitals.core.HealthCheck;

public interface HealthResultAggregator {
    HealthCheck.HealthCheckResult aggregate(Map<HealthCheck, HealthCheck.HealthCheckResult> results);

    String getName();
}
