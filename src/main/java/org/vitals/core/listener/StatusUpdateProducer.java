package org.vitals.core.listener;

import java.util.Iterator;

import org.vitals.core.filter.HealthCheckFilter;

public interface StatusUpdateProducer {

    /**
     * Remove all listeners
     */
    void clear();

    /**
     * Add a single listener for all events
     *
     * @param listener The listener to add
     */
    void addListener(final StatusUpdateListener listener);

    /**
     * Add a single listener with a filter
     *
     * @param listener The listener to add
     * @param filter   The filter to apply to the listener
     */
    void addListener(final StatusUpdateListener listener, final HealthCheckFilter filter);

    /**
     * Remove a single listener
     *
     * @param listener The listener to remove
     */
    void removeListener(final StatusUpdateListener listener);

    /**
     * Allow iterating over all the added listeners
     */
    Iterator<StatusUpdateListener> listeners();

    boolean isListenerRegistered(StatusUpdateListener listener);
}
