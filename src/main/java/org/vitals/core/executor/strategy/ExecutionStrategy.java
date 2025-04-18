package org.vitals.core.executor.strategy;

import java.util.concurrent.CompletableFuture;

/**
 * Defines a strategy for executing tasks.
 */
@FunctionalInterface
public interface ExecutionStrategy {

    /**
     * Executes a task with strategy.
     *
     * @param <T>      the type of healthCheckResult produced by the task
     * @param supplier a supplier that provides the task to execute
     * @return a {@link CompletableFuture} containing the healthCheckResult of the
     * task
     * @throws Exception if the task fails
     */
    <T> CompletableFuture<T> executeWithStrategy(CheckedSupplier<CompletableFuture<T>> supplier) throws Exception;

    /**
     * Represents a supplier that may throw a checked exception during execution.
     *
     * @param <T> the type of the supplied healthCheckResult
     */
    @FunctionalInterface
    interface CheckedSupplier<T> {
        /**
         * Supplies a healthCheckResult, potentially throwing an exception.
         *
         * @return the supplied healthCheckResult
         * @throws Exception if the task cannot be supplied
         */
        T get() throws Exception;
    }

}