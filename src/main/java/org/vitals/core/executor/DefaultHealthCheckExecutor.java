package org.vitals.core.executor;

import static org.vitals.core.util.Util.validateName;
import static org.vitals.core.util.Util.validateTags;

import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.vitals.core.HealthCheck;
import org.vitals.core.executor.strategy.ExecutionStrategy;
import org.vitals.core.executor.strategy.NoOpExecutionStrategy;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.history.HealthCheckHistory;
import org.vitals.core.listener.StatusUpdateDelegate;
import org.vitals.core.registry.HealthCheckRegistry;
import org.vitals.core.scheduler.VitalsScheduler;

import com.google.common.base.Preconditions;

public class DefaultHealthCheckExecutor implements HealthCheckExecutor, AutoCloseable {

    private final HealthCheckRegistry healthCheckRegistry;
    private final StatusUpdateDelegate statusUpdateDelegate;
    @SuppressWarnings("unused")
    private final ExecutionStrategy executionStrategy;
    private final HealthCheckHistory healthCheckHistory;
    private final VitalsScheduler forkJoinScheduler;

    private static final String STATUS_UPDATE_DELEGATE_NULL_MESSAGE = "Status update delegate must not be null";
    private static final String HEALTH_CHECK_REGISTRY_NULL_MESSAGE = "Health check registry must not be null";
    private static final String HEALTH_CHECK_HISTORY_NULL_MESSAGE = "Health check history must not be null";

    public DefaultHealthCheckExecutor(ExecutionStrategy executionStrategy, StatusUpdateDelegate statusUpdateDelegate,
            HealthCheckRegistry registry,
            HealthCheckHistory healthCheckHistory,
            VitalsScheduler forkJoinScheduler) {
        this.healthCheckRegistry = Preconditions.checkNotNull(registry, HEALTH_CHECK_REGISTRY_NULL_MESSAGE);
        this.statusUpdateDelegate = Preconditions.checkNotNull(statusUpdateDelegate,
                STATUS_UPDATE_DELEGATE_NULL_MESSAGE);
        this.healthCheckHistory = Preconditions.checkNotNull(healthCheckHistory,
                HEALTH_CHECK_HISTORY_NULL_MESSAGE);

        this.forkJoinScheduler = Preconditions.checkNotNull(forkJoinScheduler, "Fork join scheduler must not be null");
        this.executionStrategy = Preconditions.checkNotNull(executionStrategy, "Execution strategy must not be null");
    }

    public DefaultHealthCheckExecutor(HealthCheckRegistry registry, StatusUpdateDelegate statusUpdateDelegate,
            HealthCheckHistory healthCheckHistory, VitalsScheduler forkJoinScheduler) {
        this(new NoOpExecutionStrategy(),
                Preconditions.checkNotNull(statusUpdateDelegate, STATUS_UPDATE_DELEGATE_NULL_MESSAGE),
                Preconditions.checkNotNull(registry, HEALTH_CHECK_REGISTRY_NULL_MESSAGE),
                Preconditions.checkNotNull(healthCheckHistory,
                        HEALTH_CHECK_HISTORY_NULL_MESSAGE),
                Preconditions.checkNotNull(forkJoinScheduler, "Fork join scheduler must not be null"));
    }

    public DefaultHealthCheckExecutor(HealthCheckRegistry registry, StatusUpdateDelegate statusUpdateDelegate,
            HealthCheckHistory healthCheckHistory) {
        this(new NoOpExecutionStrategy(),
                Preconditions.checkNotNull(statusUpdateDelegate, STATUS_UPDATE_DELEGATE_NULL_MESSAGE),
                Preconditions.checkNotNull(registry, HEALTH_CHECK_REGISTRY_NULL_MESSAGE),
                Preconditions.checkNotNull(healthCheckHistory,
                        HEALTH_CHECK_HISTORY_NULL_MESSAGE),
                VitalsScheduler.getInstance());
    }

    @Override
    public CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull String healthCheckName) {
        validateName(healthCheckName);

        Optional<HealthCheck> optionalHealthCheck = this.healthCheckRegistry.getHealthCheck(healthCheckName);
        HealthCheck healthCheck = optionalHealthCheck.orElseThrow(
                () -> new IllegalArgumentException("Health check not found: " + healthCheckName));

        return executeAsyncHelper(healthCheck);
    }

    @Override
    public CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull HealthCheck healthCheck) {
        Preconditions.checkNotNull(healthCheck, "Health check must not be null");

        return executeAsyncHelper(healthCheck);
    }

    @Override
    public Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAsync(@Nonnull HealthCheckFilter filter) {
        Preconditions.checkNotNull(filter, "Health check filter must not be null");

        Set<HealthCheck> healthChecks = this.healthCheckRegistry.filterHealthChecks(filter);
        if (healthChecks.isEmpty()) {
            return Collections.emptySet();
        }

        return healthChecks.stream().map(this::executeAsyncHelper).collect(Collectors.toSet());
    }

    @Override
    public Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAll() {
        return this.healthCheckRegistry.getAllHealthChecks().stream().map(this::executeAsync)
                .collect(Collectors.toSet());
    }

    @Override
    public void close() {
        this.forkJoinScheduler.shutdown();
    }

    private CompletableFuture<HealthCheck.HealthCheckResult> executeAsyncHelper(@Nonnull HealthCheck healthCheck) {
        return CompletableFuture.supplyAsync(() -> {
            Instant startTime = Instant.now();
            try {
                // Execute health check with retry strategy
                HealthCheck.HealthCheckResult result = healthCheck.check();

                Duration executionDuration = Duration.between(startTime, Instant.now());

                ExecutionResult extendedResult = new ExecutionResult.Builder().from(result)
                        .name(healthCheck.getName())
                        .tags(healthCheck.getTags())
                        .executionDuration(executionDuration)
                        .build();

                this.healthCheckHistory.addHistoryInternal(healthCheck, extendedResult);

                return extendedResult;
            } catch (InterruptedException e) {
                // Re-interrupt the thread
                Thread.currentThread().interrupt();

                String errorMessage = "Execution error -> Execution was interrupted: " + e.getMessage();

                HealthCheck.HealthCheckResult result = new ExecutionResult.Builder()
                        .status(HealthCheck.HealthStatus.UNHEALTHY)
                        .message(errorMessage)
                        .error(e)
                        .name(healthCheck.getName())
                        .tags(healthCheck.getTags())
                        .executionDuration(Duration.ZERO) // Assuming no duration in case of failure
                        .build();

                this.healthCheckHistory.addHistoryInternal(healthCheck, result);
                this.statusUpdateDelegate.onHealthCheckFailed(healthCheck.getName(), healthCheck.getTags(), healthCheck,
                        errorMessage, e);

                return result;

            } catch (Exception e) {
                String errorMessage = e.getMessage() != null ? e.getMessage() : "Unknown error occurred";

                HealthCheck.HealthCheckResult result = new ExecutionResult.Builder()
                        .status(HealthCheck.HealthStatus.UNHEALTHY)
                        .message(errorMessage)
                        .error(e)
                        .name(healthCheck.getName())
                        .tags(healthCheck.getTags())
                        .executionDuration(Duration.ZERO)
                        .build();

                this.healthCheckHistory.addHistoryInternal(healthCheck, result);
                this.statusUpdateDelegate.onHealthCheckFailed(healthCheck.getName(), healthCheck.getTags(), healthCheck,
                        errorMessage, e);

                return result;
            }
        }, forkJoinScheduler::execute);
    }

    public static class ExecutionResult extends HealthCheck.HealthCheckResult {

        private final String healthCheckName;
        private final Set<String> tags;
        private final Instant timestamp;
        private final Duration executionDuration;
        private final Instant expirationTime;

        private ExecutionResult(Builder builder) {
            super(builder);
            this.healthCheckName = builder.name;
            this.tags = builder.tags != null ? Set.copyOf(builder.tags) : Collections.emptySet();
            this.timestamp = Instant.now();
            this.executionDuration = builder.executionDuration != null ? builder.executionDuration : Duration.ZERO;
            this.expirationTime = this.timestamp.plus(getTimeToLive());
        }

        // Getters
        public String getHealthCheckName() {
            return healthCheckName;
        }

        public Set<String> getTags() {
            return tags;
        }

        public Instant getTimestamp() {
            return timestamp;
        }

        public Duration getExecutionDuration() {
            return executionDuration;
        }

        public Instant getExpirationTime() {
            return expirationTime;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(timestamp.plus(getTimeToLive()));
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (!(o instanceof ExecutionResult that))
                return false;
            if (!super.equals(o))
                return false;
            return Objects.equals(healthCheckName, that.healthCheckName) && Objects.equals(tags,
                    that.tags) && Objects.equals(timestamp, that.timestamp)
                    && Objects.equals(executionDuration,
                            that.executionDuration)
                    && Objects.equals(expirationTime, that.expirationTime);
        }

        @Override
        public int hashCode() {
            return Objects.hash(super.hashCode(), healthCheckName, tags, timestamp, executionDuration, expirationTime);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("ExecutionResult{");
            sb.append("healthCheckName='").append(healthCheckName).append('\'');
            sb.append(", tags=").append(tags);
            sb.append(", timestamp=").append(timestamp);
            sb.append(", executionDuration=").append(executionDuration);
            sb.append(", expirationTime=").append(expirationTime);
            sb.append('}');
            return sb.toString();
        }

        private static class Builder extends HealthCheck.HealthCheckResult.Builder<Builder> {
            private String name;
            private Set<String> tags;
            private Duration executionDuration;

            public Builder name(String name) {
                validateName(name);
                this.name = name;
                return self();
            }

            public Builder tags(Set<String> tags) {
                validateTags(tags);
                this.tags = tags;
                return self();
            }

            public Builder executionDuration(Duration executionDuration) {
                this.executionDuration = Preconditions.checkNotNull(executionDuration,
                        "Execution duration must not be null");
                return self();
            }

            @Override
            protected Builder self() {
                return this;
            }

            @Override
            public ExecutionResult build() {
                return new ExecutionResult(this);
            }
        }
    }

}