package org.vitals.core.scheduler;

import java.util.concurrent.TimeUnit;

import jakarta.annotation.Nonnull;

public interface HealthCheckScheduler {

    void schedule(@Nonnull String healthCheckName, long initialDelay, long delay, @Nonnull TimeUnit timeUnit);

    void scheduleWithCron(@Nonnull String healthCheckName, @Nonnull String cronExpression);

    boolean isScheduled(@Nonnull String healthCheckName);

    void cancelScheduledHealthCheck(@Nonnull String healthCheckName);

}
