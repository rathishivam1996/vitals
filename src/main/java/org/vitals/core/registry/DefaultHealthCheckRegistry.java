package org.vitals.core.registry;

import com.google.common.base.Preconditions;
import jakarta.annotation.Nonnull;
import org.vitals.core.HealthCheck;
import org.vitals.core.aggregator.HealthResultAggregator;
import org.vitals.core.event.AllHealthChecksClearedEvent;
import org.vitals.core.event.HealthCheckRegisteredEvent;
import org.vitals.core.event.HealthCheckRemovedEvent;
import org.vitals.core.event.HealthEventPublisher;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.stream.Collectors;

import static org.vitals.core.util.Util.validateName;

public class DefaultHealthCheckRegistry implements HealthCheckRegistry {

    private final ConcurrentMap<String, HealthCheck> healthChecks;
    private final ConcurrentMap<String, HealthResultAggregator> aggregators;
    private final HealthEventPublisher domainEventPublisher;

    public DefaultHealthCheckRegistry(HealthEventPublisher domainEventPublisher) {
        this.healthChecks = new ConcurrentHashMap<>();
        this.aggregators = new ConcurrentHashMap<>();
        this.domainEventPublisher = domainEventPublisher;
    }

    @Override
    public synchronized boolean registerHealthCheck(@Nonnull HealthCheck healthCheck) {
        Preconditions.checkNotNull(healthCheck, "Health check must not be null");

        HealthCheck existing = this.healthChecks.putIfAbsent(healthCheck.getName(), healthCheck);
        if (existing == null) {
            this.domainEventPublisher
                    .publish(new HealthCheckRegisteredEvent(healthCheck.getName(), healthCheck.getTags(), healthCheck));
            return true;
        }
        return false;

    }

    @Override
    public boolean isHealthCheckRegistered(@Nonnull String name) {
        validateName(name);

        return this.healthChecks.containsKey(name);
    }

    @Override
    public synchronized Optional<HealthCheck> unregisterHealthCheck(@Nonnull String name) {
        validateName(name);
        HealthCheck removed = this.healthChecks.remove(name);
        if (removed != null) {
            this.domainEventPublisher.publish(new HealthCheckRemovedEvent(name, removed.getTags(), removed));
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    @Override
    public synchronized Optional<Set<HealthCheck>> unregisterHealthCheck(@Nonnull HealthCheckFilter filter) {
        Set<Map.Entry<String, HealthCheck>> matchedEntries = this.healthChecks.entrySet()
                .stream()
                .filter(entry -> filter.matches(createContext(entry.getValue())))
                .collect(Collectors.toSet());

        if (matchedEntries.isEmpty()) {
            return Optional.empty();
        }

        Set<HealthCheck> removedChecks = new HashSet<>();
        for (Map.Entry<String, HealthCheck> entry : matchedEntries) {
            String name = entry.getKey();
            HealthCheck removed = this.healthChecks.remove(name);
            if (removed != null) {
                this.domainEventPublisher.publish(new HealthCheckRemovedEvent(name, removed.getTags(), removed));
                removedChecks.add(removed);
            }
        }

        return Optional.of(removedChecks);
    }

    @Override
    public Optional<HealthCheck> unregisterHealthCheck(@Nonnull HealthCheck healthCheck) {
        Preconditions.checkNotNull(healthCheck, "Health check must not be null");
        return this.unregisterHealthCheck(healthCheck.getName());
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(@Nonnull String name) {
        validateName(name);
        return Optional.ofNullable(this.healthChecks.get(name));
    }

    @Override
    public Set<HealthCheck> getAllHealthChecks() {
        return Set.copyOf(this.healthChecks.values());
    }

    @Override
    public Set<HealthCheck> filterHealthChecks(@Nonnull HealthCheckFilter filter) {
        return this.healthChecks.values()
                .stream()
                .filter(healthCheck -> filter.matches(createContext(healthCheck)))
                .collect(Collectors.toSet());
    }

    @Override
    public synchronized void clearAllHealthChecks() {
        this.healthChecks.clear();
        this.domainEventPublisher.publish(new AllHealthChecksClearedEvent());
    }

    private HealthCheckFilterContext createContext(HealthCheck healthCheck) {
        return new HealthCheckFilterContext(healthCheck.getName(), healthCheck, null, healthCheck.getTags());
    }

    // @Override
    // public void addListener(@Nonnull StatusUpdateListener listener) {
    // Preconditions.checkNotNull(listener, "Listener must not be null");
    //
    // this.statusUpdateDelegate.addListener(listener);
    // }
    //
    // @Override
    // public void removeListener(@Nonnull StatusUpdateListener listener) {
    // Preconditions.checkNotNull(listener, "Listener must not be null");
    //
    // this.statusUpdateDelegate.removeListener(listener);
    // }

    @Override
    public boolean registerAggregator(@Nonnull HealthResultAggregator healthResultAggregator) {
        Preconditions.checkNotNull(healthResultAggregator, "Health result aggregator must not be null");

        return this.aggregators.putIfAbsent(healthResultAggregator.getName(), healthResultAggregator) == null;
    }

    @Override
    public boolean isAggregatorRegistered(@Nonnull String name) {
        this.validateAggregatorName(name);

        return this.aggregators.containsKey(name);
    }

    @Override
    public Optional<HealthResultAggregator> unregisterAggregator(@Nonnull String name) {
        this.validateAggregatorName(name);

        HealthResultAggregator removed = this.aggregators.remove(name);
        if (removed != null) {
            return Optional.of(removed);
        }
        return Optional.empty();
    }

    @Override
    public Optional<HealthResultAggregator> getAggregator(@Nonnull String name) {
        this.validateAggregatorName(name);
        return Optional.ofNullable(this.aggregators.get(name));
    }

    @Override
    public Set<HealthResultAggregator> getAllAggregators() {
        return Set.copyOf(this.aggregators.values());
    }

    @Override
    public void clearAllAggregators() {
        this.aggregators.clear();
    }

    private void validateAggregatorName(String name) {
        Preconditions.checkNotNull(name, "Health result aggregator name cannot be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Health result aggregator name must not be empty");
    }

}