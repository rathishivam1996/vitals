package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;

import java.util.Set;

public record HealthCheckStatusChangedEvent(
        @Nonnull String name, @Nonnull Set<String> tags,
        @Nonnull HealthCheck healthCheck,
        @Nullable HealthCheckResult original,
        @Nonnull HealthCheckResult updated
) implements HealthEvent {
}