package org.vitals.core.executor;

import jakarta.annotation.Nonnull;
import org.vitals.core.HealthCheck;
import org.vitals.core.filter.HealthCheckFilter;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

public interface HealthCheckExecutor {

    CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull String healthCheckName);

    CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull HealthCheck healthCheck);

    Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAsync(@Nonnull HealthCheckFilter filter);

    Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAll();

}
