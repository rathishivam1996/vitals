package org.vitals.core.filter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;

import org.vitals.core.HealthCheck;

import com.google.common.base.Preconditions;

public class HealthCheckFilters {

    private HealthCheckFilters() {
        // Utility class: prevent instantiation
    }

    public static HealthCheckFilter byName(String name) {
        Preconditions.checkNotNull(name, "Name cannot be null");
        return context -> name.equals(context.healthCheckName());
    }

    public static HealthCheckFilter byNames(Set<String> names) {
        Preconditions.checkNotNull(names, "Names cannot be null");
        return context -> names.contains(context.healthCheckName());
    }

    public static HealthCheckFilter byStatus(HealthCheck.HealthStatus status) {
        Preconditions.checkNotNull(status, "Status cannot be null");
        return context -> context.healthCheckResult() != null && status == context.healthCheckResult().getStatus();
    }

    public static HealthCheckFilter byStatuses(Set<HealthCheck.HealthStatus> statuses) {
        Preconditions.checkNotNull(statuses, "Statuses cannot be null");
        return context -> context.healthCheckResult() != null
                && statuses.contains(context.healthCheckResult().getStatus());
    }

    public static HealthCheckFilter byAnyTag(Set<String> tags) {
        Preconditions.checkNotNull(tags, "Tags cannot be null");
        return context -> !Collections.disjoint(context.tags(), tags);
    }

    public static HealthCheckFilter byAllTags(Set<String> tags) {
        Preconditions.checkNotNull(tags, "Tags cannot be null");
        return context -> context.tags().containsAll(tags);
    }

    public static HealthCheckFilter byCustom(Predicate<HealthCheckFilterContext> predicate) {
        Preconditions.checkNotNull(predicate, "Predicate cannot be null");
        return predicate::test;
    }

    public static HealthCheckFilter byCustom(String description, Predicate<HealthCheckFilterContext> predicate) {
        Preconditions.checkNotNull(predicate, "Predicate cannot be null");
        return new DescriptiveFilter(description, predicate);
    }

    /**
     * Inner class for descriptive custom filters.
     */
    private record DescriptiveFilter(String description, Predicate<HealthCheckFilterContext> predicate)
            implements HealthCheckFilter {

        @Override
        public boolean matches(HealthCheckFilterContext context) {
            return predicate.test(context);
        }

        @Override
        public String toString() {
            return "CustomFilter: " + description;
        }
    }

    public static class HealthCheckFilterBuilder {
        private final List<HealthCheckFilter> filters = new ArrayList<>();

        public HealthCheckFilterBuilder withName(String name) {
            filters.add(HealthCheckFilters.byName(name));
            return this;
        }

        public HealthCheckFilterBuilder withNames(Set<String> names) {
            filters.add(HealthCheckFilters.byNames(names));
            return this;
        }

        public HealthCheckFilterBuilder withStatus(HealthCheck.HealthStatus status) {
            filters.add(HealthCheckFilters.byStatus(status));
            return this;
        }

        public HealthCheckFilterBuilder withStatuses(Set<HealthCheck.HealthStatus> statuses) {
            filters.add(HealthCheckFilters.byStatuses(statuses));
            return this;
        }

        public HealthCheckFilterBuilder withAnyTag(Set<String> tags) {
            filters.add(HealthCheckFilters.byAnyTag(tags));
            return this;
        }

        public HealthCheckFilterBuilder withAllTags(Set<String> tags) {
            filters.add(HealthCheckFilters.byAllTags(tags));
            return this;
        }

        public HealthCheckFilterBuilder withCustom(Predicate<HealthCheckFilterContext> predicate) {
            filters.add(HealthCheckFilters.byCustom(predicate));
            return this;
        }

        public HealthCheckFilterBuilder withCustom(String description, Predicate<HealthCheckFilterContext> predicate) {
            filters.add(HealthCheckFilters.byCustom(description, predicate));
            return this;
        }

        public HealthCheckFilter build() {
            return HealthCheckFilter.and(filters.toArray(new HealthCheckFilter[0]));
        }

    }

}
