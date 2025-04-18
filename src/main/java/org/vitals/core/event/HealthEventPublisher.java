package org.vitals.core.event;

public interface HealthEventPublisher {
    void publish(HealthEvent event);
}