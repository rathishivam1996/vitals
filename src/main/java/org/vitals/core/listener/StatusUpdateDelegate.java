package org.vitals.core.listener;

import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;
import org.vitals.core.scheduler.VitalsScheduler;

import com.google.common.base.Preconditions;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Simple delegate that can be used by {@link StatusUpdateProducer} implementers
 * as a delegate.
 * It manages the registration and notification of {@link StatusUpdateListener}
 * instances.
 */
public class StatusUpdateDelegate implements StatusUpdateListener, StatusUpdateProducer, AutoCloseable {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusUpdateDelegate.class);
    private static final String NULL_LISTENER_ERROR = "Listener cannot be null";
    private static final String NULL_FILTER_ERROR = "Filter cannot be null";

    private final Map<StatusUpdateListener, HealthCheckFilter> listenerFilters;
    private final VitalsScheduler vitalsScheduler;

    /**
     * Constructs a new StatusUpdateDelegate.
     */
    public StatusUpdateDelegate(VitalsScheduler executorService) {
        this.listenerFilters = new ConcurrentHashMap<>();
        this.vitalsScheduler = executorService;
    }

    public StatusUpdateDelegate() {
        this(VitalsScheduler.getInstance());
    }

    @Override
    public void addListener(StatusUpdateListener listener) {
        Preconditions.checkNotNull(listener, NULL_LISTENER_ERROR);

        addListener(listener, HealthCheckFilter.ALL);
        LOGGER.debug("Listener added without filter: listener={}", listener);
    }

    @Override
    public void addListener(StatusUpdateListener listener, HealthCheckFilter filter) {
        Preconditions.checkNotNull(listener, NULL_LISTENER_ERROR);
        Preconditions.checkNotNull(filter, NULL_FILTER_ERROR);

        if (listenerFilters.putIfAbsent(listener, filter) != null) { // Prevent duplicates
            LOGGER.warn("Attempted to add duplicate listener: {}", listener);
            return;
        }
        LOGGER.debug("Listener added: listener={}, filter={}", listener, filter);
    }

    @Override
    public void removeListener(StatusUpdateListener listener) {
        Preconditions.checkNotNull(listener, NULL_LISTENER_ERROR);

        if (listenerFilters.remove(listener) == null) {
            LOGGER.warn("Attempted to remove non-registered listener: {}", listener);
            return;
        }
        LOGGER.debug("Listener removed: listener={}", listener);
    }

    @Override
    public boolean isListenerRegistered(StatusUpdateListener listener) {
        return listenerFilters.containsKey(listener); // Thread-safe due to ConcurrentHashMap
    }

    @Override
    public synchronized Iterator<StatusUpdateListener> listeners() {
        return Collections.unmodifiableSet(listenerFilters.keySet()).iterator();
    }

    @Override
    public synchronized void clear() {
        listenerFilters.clear();
        LOGGER.debug("All listeners cleared.");
    }

    @Override
    public void onHealthChecked(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nonnull HealthCheck.HealthCheckResult result) {
        LOGGER.debug("Health check triggered: healthCheckName={}, healthCheckResult={}", name, result);

        HealthCheckFilterContext context = new HealthCheckFilterContext(name, healthCheck, result, tags);
        this.notifyListeners(listener -> listener.onHealthChecked(name, tags, healthCheck, result), context);
    }

    @Override
    public void onHealthCheckFailed(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable String message, @Nonnull Throwable throwable) {
        LOGGER.debug("Health check failed: healthCheckName={}, message={}, throwable={}", name, message, throwable);

        HealthCheckFilterContext context = new HealthCheckFilterContext(name, healthCheck, null, tags);
        this.notifyListeners(listener -> listener.onHealthCheckFailed(name, tags, healthCheck, message, throwable),
                context);
    }

    @Override
    public void onChanged(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable HealthCheck.HealthCheckResult original,
            @Nonnull HealthCheck.HealthCheckResult updated) {

        LOGGER.debug("Health check status changed: healthCheckName={}, original={}, updated={}", name, original,
                updated);

        HealthCheckFilterContext context = new HealthCheckFilterContext(name, healthCheck, updated, tags);
        this.notifyListeners(listener -> listener.onChanged(name, tags, healthCheck, original, updated), context);
    }

    @Override
    public void onHealthCheckAdded(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck) {
        LOGGER.debug("Health check added: healthCheckName={}", name);

        HealthCheckFilterContext context = new HealthCheckFilterContext(name, healthCheck, null, tags);
        this.notifyListeners(listener -> listener.onHealthCheckAdded(name, tags, healthCheck), context);
    }

    @Override
    public void onHealthCheckRemoved(@Nonnull String name, @Nonnull Set<String> tags,
            @Nonnull HealthCheck healthCheck) {
        LOGGER.debug("Health check removed: healthCheckName={}", name);

        HealthCheckFilterContext context = new HealthCheckFilterContext(name, healthCheck, null, tags);
        this.notifyListeners(listener -> listener.onHealthCheckRemoved(name, tags, healthCheck), context);
    }

    @Override
    public void onHealthResultAggregated(@Nonnull String aggregatorName, @Nonnull HealthCheckResult aggregatedResult) {
        HealthCheckFilterContext context = new HealthCheckFilterContext(aggregatorName, null, aggregatedResult, null);
        this.notifyListeners(listener -> listener.onHealthResultAggregated(aggregatorName, aggregatedResult), context);
    }

    @Override
    public void onAggregatedResultChanged(@Nonnull String aggregatorName,
            @Nullable HealthCheckResult previousAggregated, @Nonnull HealthCheckResult updatedAggregated) {
        HealthCheckFilterContext context = new HealthCheckFilterContext(aggregatorName, null, updatedAggregated, null);
        this.notifyListeners(
                listener -> listener.onAggregatedResultChanged(aggregatorName, previousAggregated, updatedAggregated),
                context);

    }

    private void notifyListeners(Consumer<StatusUpdateListener> action, HealthCheckFilterContext context) {
        listenerFilters.entrySet()
                .stream()
                .filter(entry -> entry.getValue().matches(context))
                .map(Map.Entry::getKey)
                .forEach(listener -> this.vitalsScheduler.execute(() -> {
                    try {
                        action.accept(listener);
                    } catch (Exception e) {
                        LOGGER.error(
                                "Callbacks should not throw exceptions. Error in listener notification: listener={}, error={}",
                                listener, e.getMessage(), e);
                    }
                }));
    }

    @Override
    public void close() throws Exception {
        this.vitalsScheduler.shutdown();
    }

}