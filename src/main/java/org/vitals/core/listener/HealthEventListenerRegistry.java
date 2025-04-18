package org.vitals.core.listener;

import org.vitals.core.event.HealthEvent;
import org.vitals.core.filter.HealthCheckFilter;

import java.util.Iterator;
import java.util.List;
import java.util.Set;

public interface HealthEventListenerRegistry {

    /**
     * Remove all listeners.
     */
    void clear();

    /**
     * Add a single listener for all events.
     *
     * @param listener The listener to add. Must not be null.
     */
    void addListener(final HealthEventListener listener);

    /**
     * Add a single listener for a specific event type.
     *
     * @param listener  The listener to add. Must not be null.
     * @param eventType The specific event type to listen to. Must not be null.
     */
    void addListener(final HealthEventListener listener, final Class<? extends HealthEvent> eventType);

    void addListener(HealthEventListener listener, Set<Class<? extends HealthEvent>> eventTypes);

    /**
     * Add a single listener for all events with a filter.
     *
     * @param listener The listener to add. Must not be null.
     * @param filter   The filter to apply to the listener. Must not be null.
     */
    void addListener(final HealthEventListener listener, final HealthCheckFilter filter);

    /**
     * Add a single listener for a specific event type with a filter.
     *
     * @param listener  The listener to add. Must not be null.
     * @param filter    The filter to apply to the listener. Must not be null.
     * @param eventType The specific event type to listen to. Must not be null.
     */
    void addListener(final HealthEventListener listener, final HealthCheckFilter filter,
                     final Class<? extends HealthEvent> eventType);

    /**
     * Add a single listener for multiple event types with a filter.
     *
     * @param listener   The listener to add. Must not be null.
     * @param filter     The filter to apply to the listener. Must not be null.
     * @param eventTypes An array of event types to listen to. Must not be null or
     *                   empty.
     */
    void addListener(final HealthEventListener listener, final HealthCheckFilter filter,
                     final Class<? extends HealthEvent>... eventTypes);

    /**
     * Add a single listener for multiple event types with a filter.
     *
     * @param listener   The listener to add. Must not be null.
     * @param filter     The filter to apply to the listener. Must not be null.
     * @param eventTypes A set of event types to listen to. Must not be null or
     *                   empty.
     */
    void addListener(final HealthEventListener listener, final HealthCheckFilter filter,
                     final Set<Class<? extends HealthEvent>> eventTypes);

    /**
     * Remove a single listener from all event types.
     *
     * @param listener The listener to remove. Must not be null.
     */
    void removeListener(final HealthEventListener listener);

    /**
     * Remove a single listener from a specific event type.
     *
     * @param listener  The listener to remove. Must not be null.
     * @param eventType The specific event type to stop listening to. Must not be
     *                  null.
     */
    void removeListener(final HealthEventListener listener, final Class<? extends HealthEvent> eventType);

    /**
     * Remove a single listener from multiple event types.
     *
     * @param listener   The listener to remove. Must not be null.
     * @param eventTypes An array of event types to stop listening to. Must not be
     *                   null or empty.
     */
    void removeListener(final HealthEventListener listener, final Class<? extends HealthEvent>... eventTypes);

    /**
     * Remove a single listener from multiple event types.
     *
     * @param listener   The listener to remove. Must not be null.
     * @param eventTypes A set of event types to stop listening to. Must not be null
     *                   or empty.
     */
    void removeListener(final HealthEventListener listener, final Set<Class<? extends HealthEvent>> eventTypes);

    /**
     * Allow iterating over all the added listeners.
     *
     * @return An {@link Iterator} of all registered {@link HealthEventListener}
     * instances.
     */
    List<HealthEventListener> listeners();

    /**
     * Allow iterating over all the added listeners for a specific event type.
     *
     * @param eventType The specific event type to retrieve listeners for. Must not
     *                  be null.
     * @return An {@link Iterator} of {@link HealthEventListener} instances
     * registered for the given event type.
     */
    List<HealthEventListener> listeners(final Class<? extends HealthEvent> eventType);

    /**
     * Checks if a specific listener is registered for any event.
     *
     * @param listener The listener to check. Must not be null.
     * @return {@code true} if the listener is registered for at least one event,
     * {@code false} otherwise.
     */
    boolean isListenerRegistered(final HealthEventListener listener);

    /**
     * Checks if a specific listener is registered for a particular event type.
     *
     * @param listener  The listener to check. Must not be null.
     * @param eventType The specific event type to check registration for. Must not
     *                  be null.
     * @return {@code true} if the listener is registered for the given event type,
     * {@code false} otherwise.
     */
    boolean isListenerRegistered(final HealthEventListener listener,
                                 final Class<? extends HealthEvent> eventType);
}