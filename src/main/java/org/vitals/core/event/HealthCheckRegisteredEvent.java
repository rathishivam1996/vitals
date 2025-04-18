package org.vitals.core.event;

import jakarta.annotation.Nonnull;
import org.vitals.core.HealthCheck;

import java.util.Set;

public record HealthCheckRegisteredEvent(
        @Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck
) implements HealthEvent {
}