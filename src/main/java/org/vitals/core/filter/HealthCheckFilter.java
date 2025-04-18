package org.vitals.core.filter;

import java.util.Arrays;

/**
 * Functional interface for filtering health checks based on various criteria.
 */
@FunctionalInterface
public interface HealthCheckFilter {

    /**
     * Filter that matches any health check.
     */
    HealthCheckFilter ALL = context -> true;

    /**
     * Combines multiple filters with logical AND.
     *
     * @param filters The filters to combine.
     * @return A filter that matches only if all the filters match.
     */
    static HealthCheckFilter and(HealthCheckFilter... filters) {
        return context -> Arrays.stream(filters).allMatch(filter -> filter.matches(context));
    }

    /**
     * Combines multiple filters with logical OR.
     *
     * @param filters The filters to combine.
     * @return A filter that matches if any of the filters match.
     */
    static HealthCheckFilter or(HealthCheckFilter... filters) {
        return context -> Arrays.stream(filters).anyMatch(filter -> filter.matches(context));
    }

    /**
     * Negates a filter.
     *
     * @param filter The filter to negate.
     * @return A filter that matches if the given filter does not match.
     */
    static HealthCheckFilter not(HealthCheckFilter filter) {
        return context -> !filter.matches(context);
    }

    /**
     * Combines multiple filters with logical OR (alias for `or`).
     *
     * @param filters The filters to combine.
     * @return A filter that matches if any of the filters match.
     */
    static HealthCheckFilter anyOf(HealthCheckFilter... filters) {
        return or(filters);
    }

    /**
     * Combines multiple filters with logical AND (alias for `and`).
     *
     * @param filters The filters to combine.
     * @return A filter that matches only if all the filters match.
     */
    static HealthCheckFilter allOf(HealthCheckFilter... filters) {
        return and(filters);
    }

    /**
     * Determines if a health check matches the filter criteria.
     *
     * @param context The filter context containing metadata about the health check.
     * @return true if the health check matches the criteria, false otherwise.
     */
    boolean matches(HealthCheckFilterContext context);

}

// @FunctionalInterface
// public interface HealthCheckFilter {
//
// /**
// * Determines if a health check matches the filter criteria.
// *
// * @param healthCheckName The healthCheckName of the health check.
// * @param tags The tags associated with the health check.
// * @param healthCheck The health check instance.
// * @param healthCheckResult The healthCheckResult of the health check.
// * @return true if the health check matches the filter criteria, false
// otherwise.
// */
// boolean matches(String healthCheckName, Set<String> tags, HealthCheck
// healthCheck, HealthCheck.HealthCheckResult healthCheckResult);
//
// /**
// * Filter that matches any health check.
// */
// HealthCheckFilter ALL = (healthCheckName, tags, healthCheck,
// healthCheckResult) -> true;
//
// /**
// * Creates a composite filter that matches any of the provided filters.
// *
// * @param filters The filters to combine.
// * @return A composite filter that matches if any of the provided filters
// match.
// */
// static HealthCheckFilter anyOf(HealthCheckFilter... filters) {
// return (healthCheckName, tags, healthCheck, healthCheckResult) -> {
//
// for (HealthCheckFilter filter : filters) {
// if (filter.matches(healthCheckName, tags, healthCheck, healthCheckResult)) {
// return true;
// }
// }
// return false;
// };
// }
//
// /**
// * Creates a filter that matches specific health check names.
// *
// * @param names The set of health check names to match.
// * @return A filter that matches the specified health check names.
// */
// static HealthCheckFilter byHealthCheckNames(Set<String> names) {
// return (healthCheckName, tags, healthCheck, healthCheckResult) ->
// names.contains(healthCheckName);
// }
//
// /**
// * Creates a filter that matches specific health statuses.
// *
// * @param statuses The set of health statuses to match.
// * @return A filter that matches the specified health statuses.
// */
// static HealthCheckFilter byHealthStatuses(Set<HealthCheck.HealthStatus>
// statuses) {
// return (healthCheckName, tags, healthCheck, healthCheckResult) ->
// statuses.contains(healthCheckResult.getStatus());
// }
//
// /**
// * Creates a filter that matches custom predicates.
// *
// * @param predicate The predicate to apply.
// * @return A filter that matches the specified predicate.
// */
// static HealthCheckFilter byPredicate(Predicate<HealthCheck.HealthCheckResult>
// predicate) {
// return (healthCheckName, tags, healthCheck, healthCheckResult) ->
// predicate.test(healthCheckResult);
// }
// }
