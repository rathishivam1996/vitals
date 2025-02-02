package org.vitals.core.listener;

import java.util.EventListener;
import java.util.Set;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;

/**
 * Callbacks which receive notification of changes to dependencies. You can add
 * a
 * StatusUpdateListener to a {@link StatusUpdateProducer}, such as
 * {@link AbstractDependencyManager}
 * or {@link DependencyPinger}.
 *
 * <p>
 * Note that the order in which callbacks are received isn't strictly defined so
 * you should not
 * rely on any particular call ordering when implementing the callback methods.
 *
 * @see AbstractStatusUpdateListener
 */
public interface StatusUpdateListener extends EventListener {

        /**
         * Triggered each and every time the status of a dependency is checked.
         *
         * @param name              The healthCheckName of the HealthCheck that was
         *                          checked.
         * @param healthCheck       The HealthCheck that was checked.
         * @param healthCheckResult The healthCheckResult of the HealthCheck.
         */
        void onHealthChecked(@Nonnull final String name, @Nonnull Set<String> tags,
                        @Nonnull final HealthCheck healthCheck,
                        @Nonnull final HealthCheckResult healthCheckResult);

        void onHealthCheckFailed(@Nonnull final String name, @Nonnull Set<String> tags,
                        @Nonnull final HealthCheck healthCheck,
                        @Nullable final String message, @Nonnull final Throwable throwable);

        /**
         * Triggered when the status of a dependency has changed.
         *
         * @param name        The healthCheckName of the HealthCheck that was checked.
         * @param healthCheck The HealthCheck that was checked.
         * @param original    The previous healthCheckResult of the HealthCheck.
         * @param updated     The new healthCheckResult of the HealthCheck.
         */
        void onChanged(@Nonnull final String name, @Nonnull Set<String> tags, @Nonnull final HealthCheck healthCheck,
                        @Nullable final HealthCheckResult original, @Nonnull final HealthCheckResult updated);

        /**
         * Triggered when a new dependency is added
         *
         * @param name        The healthCheckName of HealthCheck being added.
         * @param healthCheck The HealthCheck being added.
         */
        void onHealthCheckAdded(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck);

        /**
         * Triggered when a health result is aggregated.
         *
         * @param aggregatorName   The name of the aggregator.
         * @param aggregatedResult The aggregated health result.
         */
        void onHealthResultAggregated(@Nonnull String aggregatorName,
                        @Nonnull HealthCheck.HealthCheckResult aggregatedResult);

        /**
         * Triggered when an aggregated health result changes.
         *
         * @param aggregatorName     The name of the aggregator.
         * @param previousAggregated The previous aggregated health result.
         * @param updatedAggregated  The new aggregated health result.
         */
        void onAggregatedResultChanged(@Nonnull String aggregatorName,
                        @Nullable HealthCheck.HealthCheckResult previousAggregated,
                        @Nonnull HealthCheck.HealthCheckResult updatedAggregated);

        /**
         * Triggered when a dependency is removed
         *
         * @param name        The healthCheckName of HealthCheck being removed.
         * @param healthCheck The HealthCheck being removed.
         */
        default void onHealthCheckRemoved(@Nonnull String name, @Nonnull Set<String> tags,
                        @Nonnull HealthCheck healthCheck) {
        }

        /**
         * Triggered when all dependencies are removed
         */
        default void onAllHealthChecksCleared() {
        }

}