package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.vitals.core.HealthCheck;

import java.util.Set;

public record HealthCheckFailedEvent(
        @Nonnull String name, @Nonnull Set<String> tags,
        @Nonnull HealthCheck healthCheck, @Nullable String message,
        @Nonnull Throwable throwable
) implements HealthEvent {
}