package org.vitals.core.scheduler;

import jakarta.annotation.Nonnull;

import java.util.concurrent.TimeUnit;

public interface HealthCheckScheduler {

    void schedule(@Nonnull String healthCheckName, long initialDelay, long delay, @Nonnull TimeUnit timeUnit);

    void scheduleWithCron(@Nonnull String healthCheckName, @Nonnull String cronExpression);

    boolean isScheduled(@Nonnull String healthCheckName);

    void cancelScheduledHealthCheck(@Nonnull String healthCheckName);

}
