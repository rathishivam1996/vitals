package org.vitals.core.executor.strategy;

import dev.failsafe.Failsafe;
import dev.failsafe.FailsafeExecutor;
import dev.failsafe.RetryPolicy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;

/**
 * An implementation of {@link ExecutionStrategy} that uses the Failsafe library
 * to execute tasks with retry policies.
 */
public class FailsafeExecutionStrategy implements ExecutionStrategy {
    private final FailsafeExecutor<Object> failsafeExecutor;

    /**
     * Constructs a {@link FailsafeExecutionStrategy} with specified retry
     * parameters.
     *
     * @param maxRetries   the maximum number of retry attempts
     * @param initialDelay the initial delay between retries
     * @param maxDelay     the maximum delay between retries
     * @throws IllegalArgumentException if any parameter is invalid (e.g., negative
     *                                  delays or retries)
     */
    public FailsafeExecutionStrategy(int maxRetries, Duration initialDelay, Duration maxDelay, Duration jitter) {
        if (maxRetries < 0 || initialDelay.isNegative() || maxDelay.isNegative()) {
            throw new IllegalArgumentException("Retries and delays must be non-negative.");
        }

        RetryPolicy<Object> retryPolicy = RetryPolicy.builder()
                .withMaxRetries(maxRetries)
                .withBackoff(initialDelay, maxDelay)
                .withJitter(jitter)
                .handle(Exception.class) // Retry on all exceptions
                .build();

        this.failsafeExecutor = Failsafe.with(retryPolicy);
    }

    @Override
    public <T> CompletableFuture<T> executeWithStrategy(CheckedSupplier<CompletableFuture<T>> task) throws Exception {
        return failsafeExecutor.getStageAsync(task::get);
    }

}
