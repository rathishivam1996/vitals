package org.vitals.core.filter;

import java.util.Collections;
import java.util.Set;

import org.vitals.core.HealthCheck;

/**
 * Represents the context for filtering health checks
 */
public record HealthCheckFilterContext(
        String healthCheckName, HealthCheck healthCheck, HealthCheck.HealthCheckResult healthCheckResult,
        Set<String> tags) {
    public HealthCheckFilterContext(String healthCheckName, HealthCheck healthCheck,
            HealthCheck.HealthCheckResult healthCheckResult,
            Set<String> tags) {
        this.healthCheckName = healthCheckName;
        this.healthCheck = healthCheck;
        this.healthCheckResult = healthCheckResult;
        this.tags = tags != null ? Set.copyOf(tags) : Collections.emptySet();
    }
}