package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;

import java.util.Set;

public record HealthCheckCheckedEvent(
        @Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
        @Nonnull HealthCheckResult healthCheckResult
) implements HealthEvent {
}