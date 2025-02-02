package org.vitals.test;

import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.listener.StatusUpdateListener;

public class ConsoleStatusUpdateListener implements StatusUpdateListener {

    @Override
    public void onHealthChecked(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nonnull HealthCheckResult healthCheckResult) {
        System.out.println();
        System.out.println("Health checked: " + name + ", Tags: " + tags + ", Result: " + healthCheckResult);
        System.out.println();

    }

    @Override
    public void onHealthCheckFailed(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable String message, @Nonnull Throwable throwable) {
        System.out.println();

        System.out.println("Health check failed: " + name + ", Tags: " + tags + ", Message: " + message);
        throwable.printStackTrace(System.out);
        System.out.println();

    }

    @Override
    public void onChanged(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable HealthCheckResult original, @Nonnull HealthCheckResult updated) {
        System.out.println();

        System.out.println("Health check changed: " + name + ", Tags: " + tags + ", Original: " + original
                + ", Updated: " + updated);
        System.out.println();

    }

    @Override
    public void onHealthCheckAdded(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck) {
        System.out.println("Health check added: " + name + ", Tags: " + tags);
    }

    @Override
    public void onHealthResultAggregated(@Nonnull String aggregatorName, @Nonnull HealthCheckResult aggregatedResult) {

        System.out.println();
        System.out.println("Health result aggregated: " + aggregatorName + ", Result: " + aggregatedResult);
        System.out.println();
    }

    @Override
    public void onAggregatedResultChanged(@Nonnull String aggregatorName,
            @Nullable HealthCheckResult previousAggregated, @Nonnull HealthCheckResult updatedAggregated) {

        System.out.println();
        System.out.println("Aggregated result changed: " + aggregatorName + ", Previous: " + previousAggregated
                + ", Updated: " + updatedAggregated);
        System.out.println();
    }
}
