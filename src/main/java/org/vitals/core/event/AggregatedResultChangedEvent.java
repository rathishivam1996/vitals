package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.vitals.core.HealthCheck.HealthCheckResult;

public record AggregatedResultChangedEvent(
        @Nonnull String aggregatorName,
        @Nullable HealthCheckResult previousAggregated,
        @Nonnull HealthCheckResult updatedAggregated
) implements HealthEvent {
}