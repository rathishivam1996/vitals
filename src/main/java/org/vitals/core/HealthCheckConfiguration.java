package org.vitals.core;

import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Configuration class for health check settings, including timeout, grace
 * period, and scheduling configurations.
 * <p>
 * Instances of this class are immutable and thread-safe. Use the
 * {@link Builder} to create
 * instances with customized configurations.
 * </p>
 */
public final class HealthCheckConfiguration {

    private final Duration timeout;
    private final Duration gracePeriod;
    private final Map<String, Object> customSettings;

    // Scheduling configurations
    private final Long interval;
    private final Long initialDelay;
    private final TimeUnit timeUnit;
    private final String cronExpression;

    private HealthCheckConfiguration(Builder builder) {
        this.timeout = builder.timeout;
        this.gracePeriod = builder.gracePeriod;
        this.customSettings = Collections.unmodifiableMap(builder.customSettings);

        this.interval = builder.interval;
        this.initialDelay = builder.initialDelay;
        this.timeUnit = builder.timeUnit;
        this.cronExpression = builder.cronExpression;
    }

    public Optional<Duration> getTimeout() {
        return Optional.ofNullable(timeout);
    }

    public Optional<Duration> getGracePeriod() {
        return Optional.ofNullable(gracePeriod);
    }

    public Object getCustomSetting(String key) {
        return customSettings.get(key);
    }

    // Scheduling configurations getters
    public Optional<Long> getInterval() {
        return Optional.ofNullable(interval);
    }

    public Optional<Long> getInitialDelay() {
        return Optional.ofNullable(initialDelay);
    }

    public Optional<TimeUnit> getTimeUnit() {
        return Optional.ofNullable(timeUnit);
    }

    public Optional<String> getCronExpression() {
        return Optional.ofNullable(cronExpression);
    }

    /**
     * Builder class for {@link HealthCheckConfiguration}.
     * Ensures valid configurations with optional fields and provides default
     * values.
     */
    public static class Builder {

        private Duration timeout;
        private Duration gracePeriod;
        private Map<String, Object> customSettings = Collections.emptyMap();

        // Scheduling configurations
        private Long interval;
        private Long initialDelay;
        private TimeUnit timeUnit = TimeUnit.SECONDS;
        private String cronExpression;

        public Builder setTimeout(Duration timeout) {
            if (timeout != null && timeout.isNegative()) {
                throw new IllegalArgumentException("Timeout duration must be non-negative.");
            }
            this.timeout = timeout;
            return this;
        }

        public Builder setGracePeriod(Duration gracePeriod) {
            if (gracePeriod != null && gracePeriod.isNegative()) {
                throw new IllegalArgumentException("Grace period must be non-negative.");
            }
            this.gracePeriod = gracePeriod;
            return this;
        }

        public Builder setCustomSettings(Map<String, Object> customSettings) {
            this.customSettings = Collections.unmodifiableMap(customSettings);
            return this;
        }

        /**
         * Sets the interval between health check executions.
         *
         * @param interval Interval duration in the specified time unit.
         * @return this Builder instance
         */
        public Builder setInterval(long interval) {
            if (interval <= 0) {
                throw new IllegalArgumentException("Interval must be greater than 0.");
            }
            this.interval = interval;
            return this;
        }

        /**
         * Sets the initial delay before the first execution of the health check.
         *
         * @param initialDelay Initial delay duration in the specified time unit.
         * @return this Builder instance
         */
        public Builder setInitialDelay(long initialDelay) {
            if (initialDelay < 0) {
                throw new IllegalArgumentException("Initial delay must be non-negative.");
            }
            this.initialDelay = initialDelay;
            return this;
        }

        /**
         * Sets the time unit for interval and initial delay.
         *
         * @param timeUnit The unit of time for interval and delay.
         * @return this Builder instance
         */
        public Builder setTimeUnit(TimeUnit timeUnit) {
            this.timeUnit = timeUnit;
            return this;
        }

        /**
         * Sets a cron expression for scheduling health checks.
         * If provided, overrides interval and delay configurations.
         *
         * @param cronExpression Cron expression for scheduling.
         * @return this Builder instance
         */
        public Builder setCronExpression(String cronExpression) {
            if (cronExpression != null && cronExpression.trim().isEmpty()) {
                throw new IllegalArgumentException("Cron expression cannot be empty.");
            }
            this.cronExpression = cronExpression;
            return this;
        }

        public HealthCheckConfiguration build() {
            return new HealthCheckConfiguration(this);
        }
    }
}