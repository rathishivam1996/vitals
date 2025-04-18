package org.vitals.core.event;

public sealed interface HealthEvent permits
        HealthCheckCheckedEvent,
        HealthCheckFailedEvent,
        HealthCheckStatusChangedEvent,
        HealthCheckRegisteredEvent,
        HealthResultAggregatedEvent,
        AggregatedResultChangedEvent,
        HealthCheckRemovedEvent,
        AllHealthChecksClearedEvent {
}