package org.vitals.core.listener;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.event.*;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;
import org.vitals.core.scheduler.InternalScheduler;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simple delegate that can be used by {@link HealthEventListenerRegistry} implementers
 * as a delegate.
 * It manages the registration and notification of {@link HealthEventListener}
 * instances.
 */
public class StatusUpdateDelegate implements HealthEventListener, HealthEventListenerRegistry, HealthEventPublisher {

    private static final Logger LOGGER = LoggerFactory.getLogger(StatusUpdateDelegate.class);
    private static final String NULL_LISTENER_ERROR = "Listener cannot be null";
    private static final String NULL_FILTER_ERROR = "Filter cannot be null";

    private final Map<Class<? extends HealthEvent>, Map<HealthEventListener, HealthCheckFilter>> eventListeners;
    private final InternalScheduler internalScheduler;

    public StatusUpdateDelegate(InternalScheduler executorService) {
        this.eventListeners = new ConcurrentHashMap<>();
        this.internalScheduler = executorService;
    }

    public StatusUpdateDelegate() {
        this(InternalScheduler.getInstance());
    }

    @Override
    public void addListener(HealthEventListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        this.addListener(listener, HealthCheckFilter.ALL, HealthEvent.class);
    }

    @Override
    public void addListener(HealthEventListener listener, Class<? extends HealthEvent> eventType) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        this.addListener(listener, HealthCheckFilter.ALL, eventType);
    }

    @Override
    public void addListener(HealthEventListener listener, Set<Class<? extends HealthEvent>> eventTypes) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(eventTypes, "eventType cannot be null");
        this.addListener(listener, HealthCheckFilter.ALL, eventTypes);
    }

    @Override
    public void addListener(HealthEventListener listener, HealthCheckFilter filter) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        this.addListener(listener, filter, HealthEvent.class);
    }

    @SafeVarargs
    @Override
    public final void addListener(HealthEventListener listener, HealthCheckFilter filter,
                                  Class<? extends HealthEvent>... eventTypes) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        Objects.requireNonNull(eventTypes, "eventTypes cannot be null");
        Arrays.stream(eventTypes).forEach(eventType -> this.addListener(listener, filter, eventType));
    }

    @Override
    public void addListener(HealthEventListener listener, HealthCheckFilter filter,
                            Set<Class<? extends HealthEvent>> eventTypes) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        Objects.requireNonNull(eventTypes, "eventTypes cannot be null");
        eventTypes.forEach(eventType -> this.addListener(listener, filter, eventType));
    }

    @Override
    public void addListener(HealthEventListener listener, HealthCheckFilter filter,
                            Class<? extends HealthEvent> eventType) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(filter, "filter cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventListeners.computeIfAbsent(eventType, k -> new ConcurrentHashMap<>()).putIfAbsent(listener, filter);
        LOGGER.debug("Listener added: listener={}, filter={}, eventType={}", listener, filter, eventType.getName());
    }

    @SafeVarargs
    @Override
    public final void removeListener(HealthEventListener listener, Class<? extends HealthEvent>... eventTypes) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(eventTypes, "eventTypes cannot be null");
        Arrays.stream(eventTypes).forEach(eventType -> this.removeListener(listener, eventType));
    }

    @Override
    public void removeListener(HealthEventListener listener, Set<Class<? extends HealthEvent>> eventTypes) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(eventTypes, "eventTypes cannot be null");
        eventTypes.forEach(eventType -> this.removeListener(listener, eventType));
    }

    @Override
    public void removeListener(HealthEventListener listener, Class<? extends HealthEvent> eventType) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Objects.requireNonNull(eventType, "eventType cannot be null");
        this.eventListeners.computeIfPresent(eventType, (key, value) -> {
            if (value.remove(listener) == null) {
                LOGGER.warn("Attempted to remove non-registered listener for event type {}: {}", eventType.getName(),
                        listener);
            } else {
                LOGGER.debug("Listener removed: listener={}, eventType={}", listener, eventType.getName());
            }
            return value.isEmpty() ? null : value;
        });
    }

    @Override
    public void removeListener(HealthEventListener listener) {
        Objects.requireNonNull(listener, "listener cannot be null");
        Set<Class<? extends HealthEvent>> eventTypes = new HashSet<>(this.eventListeners.keySet());

        for (Class<? extends HealthEvent> eventType : eventTypes) {
            this.eventListeners.computeIfPresent(eventType, (key, listenerMap) -> {
                listenerMap.remove(listener);
                return listenerMap.isEmpty() ? null : listenerMap;
            });
        }
        LOGGER.debug("Listener removed from all event types: listener={}", listener);
    }

    /*
     * @Override
     * public void removeListener(StatusUpdateListener listener) {
     * Objects.requireNonNull(listener, "listener cannot be null");
     * this.removeListener(listener, HealthCheckEvent.class);
     * }
     */

    @Override
    public boolean isListenerRegistered(HealthEventListener listener) {
        List<Map<HealthEventListener, HealthCheckFilter>> snapshotOfValues = new ArrayList<>(
                this.eventListeners.values());
        return snapshotOfValues.stream().anyMatch(map -> map.containsKey(listener));
    }

    @Override
    public boolean isListenerRegistered(HealthEventListener listener, Class<? extends HealthEvent> eventType) {
        Map<HealthEventListener, HealthCheckFilter> listenerMap = this.eventListeners.get(eventType);
        return listenerMap != null && listenerMap.containsKey(listener);
    }

    @Override
    public List<HealthEventListener> listeners() {
        return this.eventListeners.values()
                .stream()
                .flatMap(map -> map.keySet().stream())
                .distinct()
                .toList();
    }

    @Override
    public List<HealthEventListener> listeners(Class<? extends HealthEvent> eventType) {
        Map<HealthEventListener, HealthCheckFilter> map = this.eventListeners.get(eventType);
        if (map == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(map.keySet());
    }

    @Override
    public void clear() {
        this.eventListeners.clear();
        LOGGER.debug("All listeners cleared.");
    }

    @Override
    public void publish(HealthEvent event) {
        Objects.requireNonNull(event, "event cannot be null");

        if (eventListeners.isEmpty())
            return;

        Set<HealthEventListener> notifiedListeners = new HashSet<>();

        Class<? extends HealthEvent> exactType = event.getClass();
        Map<HealthEventListener, HealthCheckFilter> exactTypeListeners = eventListeners.get(exactType);

        if (exactTypeListeners != null && !exactTypeListeners.isEmpty()) {

            HealthCheckFilterContext context = this.createContext(event);
            for (Map.Entry<HealthEventListener, HealthCheckFilter> entry : new HashMap<>(exactTypeListeners)
                    .entrySet()) {
                HealthEventListener listener = entry.getKey();
                if (notifiedListeners.contains(listener))
                    continue;

                HealthCheckFilter filter = entry.getValue();
                if (filter.matches(context)) {
                    notifiedListeners.add(listener);
                    this.scheduleNotification(listener, event);
                }
            }
        }

        Map<HealthEventListener, HealthCheckFilter> listenersForType = eventListeners.get(HealthEvent.class);
        if (listenersForType != null && !listenersForType.isEmpty()) {
            HealthCheckFilterContext context = this.createContext(event);

            for (Map.Entry<HealthEventListener, HealthCheckFilter> entry : new HashMap<>(listenersForType)
                    .entrySet()) {
                HealthEventListener listener = entry.getKey();
                if (notifiedListeners.contains(listener))
                    continue;

                HealthCheckFilter filter = entry.getValue();
                if (filter.matches(context)) {
                    notifiedListeners.add(listener);
                    this.scheduleNotification(listener, event);
                }
            }
        }
    }

    private void scheduleNotification(HealthEventListener listener, HealthEvent event) {
        this.internalScheduler.execute(() -> {
            try {
                this.notifyListener(listener, event);
            } catch (Exception e) {
                LOGGER.error("Error notifying listener {} for event {}: {}", listener, event.getClass().getSimpleName(),
                        e.getMessage(), e);
            }
        });
    }

    private HealthCheckFilterContext createContext(HealthEvent event) {
        if (event instanceof HealthCheckRegisteredEvent registeredEvent) {
            return new HealthCheckFilterContext(
                    registeredEvent.name(),
                    registeredEvent.healthCheck(),
                    null,
                    registeredEvent.tags()
            );
        } else if (event instanceof HealthCheckCheckedEvent checkedEvent) {
            return new HealthCheckFilterContext(
                    checkedEvent.name(),
                    checkedEvent.healthCheck(),
                    checkedEvent.healthCheckResult(),
                    checkedEvent.tags()
            );
        } else if (event instanceof HealthCheckFailedEvent failedEvent) {
            return new HealthCheckFilterContext(
                    failedEvent.name(),
                    failedEvent.healthCheck(),
                    null, // HealthCheckFailedEvent doesn't have a result field for the context here
                    failedEvent.tags()
            );
        } else if (event instanceof HealthCheckStatusChangedEvent changedEvent) {
            return new HealthCheckFilterContext(
                    changedEvent.name(),
                    changedEvent.healthCheck(),
                    changedEvent.updated(),
                    changedEvent.tags()
            );
        } else if (event instanceof HealthCheckRemovedEvent removedEvent) {
            return new HealthCheckFilterContext(
                    removedEvent.name(),
                    removedEvent.healthCheck(),
                    null,
                    removedEvent.tags()
            );
        } else if (event instanceof HealthResultAggregatedEvent aggregatedEvent) {
            return new HealthCheckFilterContext(
                    aggregatedEvent.aggregatorName(),
                    null,
                    aggregatedEvent.aggregatedResult(),
                    null
            );
        } else if (event instanceof AggregatedResultChangedEvent aggregatedChangedEvent) {
            return new HealthCheckFilterContext(
                    aggregatedChangedEvent.aggregatorName(),
                    null, // Aggregated events don't have a health check instance here
                    aggregatedChangedEvent.updatedAggregated(),
                    null // Aggregated events don't have tags here
            );
        } else if (event instanceof AllHealthChecksClearedEvent) {
            // This is already valid Java 17
            return new HealthCheckFilterContext(null, null, null, null);
        }
        return null; // Should ideally not happen if all HealthEvent types are covered
    }

    private void notifyListener(HealthEventListener listener, HealthEvent event) {
        if (event instanceof HealthCheckRegisteredEvent registeredEvent) {
            listener.onHealthCheckAdded(
                    registeredEvent.name(),
                    registeredEvent.tags(),
                    registeredEvent.healthCheck()
            );
        } else if (event instanceof HealthCheckCheckedEvent checkedEvent) {
            listener.onHealthChecked(
                    checkedEvent.name(),
                    checkedEvent.tags(),
                    checkedEvent.healthCheck(),
                    checkedEvent.healthCheckResult()
            );
        } else if (event instanceof HealthCheckFailedEvent failedEvent) {
            listener.onHealthCheckFailed(
                    failedEvent.name(),
                    failedEvent.tags(),
                    failedEvent.healthCheck(),
                    failedEvent.message(),
                    failedEvent.throwable()
            );
        } else if (event instanceof HealthCheckStatusChangedEvent changedEvent) {
            listener.onChanged(
                    changedEvent.name(),
                    changedEvent.tags(),
                    changedEvent.healthCheck(),
                    changedEvent.original(),
                    changedEvent.updated()
            );
        } else if (event instanceof HealthCheckRemovedEvent removedEvent) {
            listener.onHealthCheckRemoved(
                    removedEvent.name(),
                    removedEvent.tags(),
                    removedEvent.healthCheck()
            );
        } else if (event instanceof HealthResultAggregatedEvent aggregatedEvent) {
            listener.onHealthResultAggregated(
                    aggregatedEvent.aggregatorName(),
                    aggregatedEvent.aggregatedResult()
            );
        } else if (event instanceof AggregatedResultChangedEvent aggregatedChangedEvent) {
            listener.onAggregatedResultChanged(
                    aggregatedChangedEvent.aggregatorName(),
                    aggregatedChangedEvent.previousAggregated(),
                    aggregatedChangedEvent.updatedAggregated()
            );
        } else if (event instanceof AllHealthChecksClearedEvent) {
            listener.onAllHealthChecksCleared();
        }
    }

    @Override
    public void onHealthChecked(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
                                @Nonnull HealthCheckResult result) {
        this.publish(new HealthCheckCheckedEvent(name, tags, healthCheck, result));
    }

    @Override
    public void onHealthCheckFailed(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
                                    @Nullable String message, @Nonnull Throwable throwable) {
        this.publish(new HealthCheckFailedEvent(name, tags, healthCheck, message, throwable));
    }

    @Override
    public void onChanged(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
                          @Nullable HealthCheckResult original, @Nonnull HealthCheckResult updated) {
        this.publish(new HealthCheckStatusChangedEvent(name, tags, healthCheck, original, updated));
    }

    @Override
    public void onHealthCheckAdded(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck) {
        this.publish(new HealthCheckRegisteredEvent(name, tags, healthCheck));
    }

    @Override
    public void onHealthCheckRemoved(@Nonnull String name, @Nonnull Set<String> tags,
                                     @Nonnull HealthCheck healthCheck) {
        this.publish(new HealthCheckRemovedEvent(name, tags, healthCheck));
    }

    @Override
    public void onHealthResultAggregated(@Nonnull String aggregatorName, @Nonnull HealthCheckResult aggregatedResult) {
        this.publish(new HealthResultAggregatedEvent(aggregatorName, aggregatedResult));
    }

    @Override
    public void onAggregatedResultChanged(@Nonnull String aggregatorName,
                                          @Nullable HealthCheckResult previousAggregated,
                                          @Nonnull HealthCheckResult updatedAggregated) {
        this.publish(new AggregatedResultChangedEvent(aggregatorName, previousAggregated, updatedAggregated));
    }

    @Override
    public void onAllHealthChecksCleared() {
        this.publish(new AllHealthChecksClearedEvent());
    }

}