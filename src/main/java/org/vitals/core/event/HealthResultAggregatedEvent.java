package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import org.vitals.core.HealthCheck.HealthCheckResult;

public record HealthResultAggregatedEvent(
        @Nonnull String aggregatorName,
        @Nonnull HealthCheckResult aggregatedResult
) implements HealthEvent {
}