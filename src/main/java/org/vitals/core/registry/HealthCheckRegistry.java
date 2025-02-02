package org.vitals.core.registry;

import java.util.Optional;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.vitals.core.aggregator.HealthResultAggregator;
import org.vitals.core.HealthCheck;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.listener.StatusUpdateListener;

public interface HealthCheckRegistry {

    boolean registerHealthCheck(@Nonnull HealthCheck healthCheck);

    boolean isHealthCheckRegistered(@Nonnull String name);

    Optional<HealthCheck> unregisterHealthCheck(@Nonnull String name);

    Optional<HealthCheck> unregisterHealthCheck(@Nonnull HealthCheck healthCheck);

    Optional<Set<HealthCheck>> unregisterHealthCheck(@Nonnull HealthCheckFilter filter);

    Optional<HealthCheck> getHealthCheck(@Nonnull String name);

    Set<HealthCheck> getAllHealthChecks();

    Set<HealthCheck> filterHealthChecks(@Nonnull HealthCheckFilter filter);

    void clearAllHealthChecks();

    void addListener(@Nonnull StatusUpdateListener listener);

    void removeListener(@Nonnull StatusUpdateListener listener);

    boolean registerAggregator(@Nonnull HealthResultAggregator aggregator);

    boolean isAggregatorRegistered(@Nonnull String name);

    Optional<HealthResultAggregator> unregisterAggregator(@Nonnull String name);

    Optional<HealthResultAggregator> getAggregator(@Nonnull String name);

    Set<HealthResultAggregator> getAllAggregators();

    void clearAllAggregators();

}