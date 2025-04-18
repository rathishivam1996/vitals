package org.vitals.core.scheduler;

import com.google.common.base.Preconditions;
import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitals.core.executor.HealthCheckExecutor;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class DefaultHealthCheckScheduler implements AutoCloseable, HealthCheckScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultHealthCheckScheduler.class);

    private final InternalScheduler internalScheduler;
    private final HealthCheckExecutor healthCheckExecutor;
    private final Map<String, ScheduledFuture<?>> scheduledTasks;

    public DefaultHealthCheckScheduler(HealthCheckExecutor healthCheckExecutor, InternalScheduler internalScheduler) {
        this.internalScheduler = internalScheduler;
        this.healthCheckExecutor = healthCheckExecutor;
        this.scheduledTasks = new ConcurrentHashMap<>();
    }

    public DefaultHealthCheckScheduler(HealthCheckExecutor healthCheckExecutor) {
        this(healthCheckExecutor, InternalScheduler.getInstance());
    }

    /**
     * Schedules a health check with a fixed delay between executions.
     *
     * @param healthCheckName the healthCheckName of the health check
     * @param initialDelay    the initial delay before the first execution
     * @param delay           the delay between the end of one execution and the
     *                        start of the next
     * @param timeUnit        the time unit of the initial delay and delay
     *                        parameters
     */
    @Override
    public void schedule(@Nonnull String healthCheckName, long initialDelay, long delay, @Nonnull TimeUnit timeUnit) {
        Preconditions.checkNotNull(healthCheckName, "Health check healthCheckName must not be null");
        Preconditions.checkArgument(!healthCheckName.trim().isEmpty(),
                "Health check healthCheckName must not be empty");
        Preconditions.checkArgument(initialDelay >= 0, "Initial delay must be non-negative");
        Preconditions.checkArgument(delay > 0, "Delay must be greater than zero");
        Preconditions.checkNotNull(timeUnit, "Time unit must not be null");

        if (this.scheduledTasks.containsKey(healthCheckName)) {
            throw new IllegalStateException("Health check is already scheduled: " + healthCheckName);
        }

        try {
            ScheduledFuture<?> scheduledFuture = this.internalScheduler.scheduleWithFixedDelay(
                    () -> this.healthCheckExecutor.executeAsync(healthCheckName)
                            .thenAccept(result -> LOGGER.info("Scheduled health check executed: {}", result))
                            .exceptionally(ex -> {
                                LOGGER.error("Exception occurred while scheduling health check [{}]: {}",
                                        healthCheckName, ex.getMessage(), ex);
                                return null;
                            }),
                    initialDelay, delay, timeUnit);

            this.scheduledTasks.put(healthCheckName, scheduledFuture);
        } catch (Exception e) {
            LOGGER.error("Exception occurred while scheduling health check [{}]: {}", healthCheckName, e.getMessage(),
                    e);
        }
    }

    /**
     * Cancels a scheduled health check.
     *
     * @param healthCheckName the healthCheckName of the health check to cancel
     */
    @Override
    public void cancelScheduledHealthCheck(@Nonnull String healthCheckName) {
        Preconditions.checkNotNull(healthCheckName, "Health check name must not be null");

        ScheduledFuture<?> future = this.scheduledTasks.remove(healthCheckName);
        if (future != null) {
            future.cancel(true);
            LOGGER.info("Health check [{}] scheduling canceled", healthCheckName);
        } else {
            LOGGER.warn("Health check [{}] was not scheduled", healthCheckName);
        }
    }

    /**
     * Checks if a health check is already scheduled.
     *
     * @param healthCheckName the healthCheckName of the health check
     * @return true if the health check is scheduled, false otherwise
     */
    @Override
    public boolean isScheduled(@Nonnull String healthCheckName) {
        return this.scheduledTasks.containsKey(healthCheckName);
    }

    /**
     * Schedules a health check using a cron expression.
     *
     * @param healthCheckName the healthCheckName of the health check
     * @param cronExpression  the cron expression defining the schedule
     */
    @Override
    public void scheduleWithCron(@Nonnull String healthCheckName, @Nonnull String cronExpression) {
        // Implementation for scheduling with cron expression
    }

    @Override
    public void close() {
        this.internalScheduler.shutdown();
    }

}