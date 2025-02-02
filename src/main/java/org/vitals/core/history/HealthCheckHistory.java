package org.vitals.core.history;

import java.util.List;
import java.util.Set;

import jakarta.annotation.Nonnull;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.filter.HealthCheckFilter;

public interface HealthCheckHistory {

    void addHistoryInternal(HealthCheck healthCheck, HealthCheckResult result);

    /**
     * Retrieves the execution history of a specific health check.
     *
     * @param name the name of the health check, or aggregator
     * @return a list of past results, or an empty list if not found
     */
    List<HealthCheckResult> getHistory(@Nonnull String name);

    /**
     * Filters health check statuses by a specific status.
     *
     * @param healthCheckFilter the filter to apply
     * @return a set of filtered health check results
     */
    Set<HealthCheck.HealthCheckResult> filterHistory(@Nonnull HealthCheckFilter healthCheckFilter);

    /**
     * Clears all stored statuses and history.
     */
    void clearHistory();

}