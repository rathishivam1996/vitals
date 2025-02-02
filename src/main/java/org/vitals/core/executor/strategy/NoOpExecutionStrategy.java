package org.vitals.core.executor.strategy;

import java.util.concurrent.CompletableFuture;

public class NoOpExecutionStrategy implements ExecutionStrategy {

    @Override
    public <T> CompletableFuture<T> executeWithStrategy(CheckedSupplier<CompletableFuture<T>> supplier)
            throws Exception {
        return supplier.get(); // Simply execute the supplier without retries
    }

}