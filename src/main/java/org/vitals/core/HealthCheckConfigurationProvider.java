package org.vitals.core;

/**
 * Interface for providing health check configurations.
 */
public interface HealthCheckConfigurationProvider {

    /**
     * Retrieves the configuration for a specific health check.
     *
     * @return The configuration for the specified health check.
     */
    HealthCheckConfiguration getConfiguration();
}