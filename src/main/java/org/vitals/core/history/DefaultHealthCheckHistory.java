package org.vitals.core.history;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.stream.Collectors;

import jakarta.annotation.Nonnull;

import org.vitals.core.aggregator.HealthResultAggregator;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;
import org.vitals.core.listener.StatusUpdateDelegate;
import org.vitals.core.registry.HealthCheckRegistry;

import com.google.common.base.Preconditions;

public class DefaultHealthCheckHistory implements HealthCheckHistory {

    private final int maxHistorySize;
    private final Map<String, LinkedBlockingDeque<HealthCheckResult>> historyMap;
    private final StatusUpdateDelegate statusUpdateDelegate;
    private final HealthCheckRegistry healthCheckRegistry;

    public DefaultHealthCheckHistory(int maxHistorySize, StatusUpdateDelegate statusUpdateDelegate,
            HealthCheckRegistry healthCheckRegistry) {
        this.maxHistorySize = maxHistorySize;
        this.historyMap = new ConcurrentHashMap<>();
        this.statusUpdateDelegate = statusUpdateDelegate;
        this.healthCheckRegistry = healthCheckRegistry;
    }

    public synchronized void addHistoryInternal(HealthCheck healthCheck, HealthCheckResult result) {
        Preconditions.checkNotNull(healthCheck, "Health check must not be null");
        Preconditions.checkNotNull(result, "Health check result must not be null");

        LinkedBlockingDeque<HealthCheckResult> historyQueue = this.historyMap.computeIfAbsent(healthCheck.getName(),
                k -> new LinkedBlockingDeque<>(maxHistorySize));

        HealthCheckResult latestResult = historyQueue.peekLast();

        while (historyQueue.size() >= maxHistorySize) {
            historyQueue.pollFirst();
        }

        historyQueue.offerLast(result);

        this.statusUpdateDelegate.onHealthChecked(healthCheck.getName(), healthCheck.getTags(), healthCheck,
                result);

        if (!Objects.equals(latestResult, result)) {
            this.statusUpdateDelegate.onChanged(
                    healthCheck.getName(),
                    healthCheck.getTags(),
                    healthCheck,
                    latestResult,
                    result);
        }

        // Get the latest results of all checks for aggregation
        Map<HealthCheck, HealthCheckResult> latestResults = new HashMap<>();

        this.historyMap.forEach((name, results) -> {
            HealthCheckResult latest = results.peekLast();
            if (latest != null) {
                // Check if the name corresponds to a HealthCheck
                this.healthCheckRegistry.getHealthCheck(name).ifPresent(hc -> latestResults.put(hc, latest));
            }
        });

        Set<HealthResultAggregator> aggregators = this.healthCheckRegistry.getAllAggregators();

        for (HealthResultAggregator aggregator : aggregators) {
            String aggregatorName = aggregator.getName();

            // Get the current aggregated result
            LinkedBlockingDeque<HealthCheckResult> aggregatedQueue = this.historyMap
                    .computeIfAbsent(aggregatorName, k -> new LinkedBlockingDeque<>(maxHistorySize));

            HealthCheckResult previousAggregated = aggregatedQueue.peekLast();
            HealthCheckResult newAggregated = aggregator.aggregate(latestResults);

            // Add the new aggregated result to the history
            while (aggregatedQueue.size() >= maxHistorySize) {
                aggregatedQueue.pollFirst();
            }
            aggregatedQueue.offerLast(newAggregated);

            // Notify that the aggregation occurred
            this.statusUpdateDelegate.onHealthResultAggregated(aggregatorName, newAggregated);

            // If the aggregated result has changed, notify the change
            if (!Objects.equals(previousAggregated, newAggregated)) {
                this.statusUpdateDelegate.onAggregatedResultChanged(
                        aggregatorName,
                        previousAggregated,
                        newAggregated);
            }
        }
    }

    @Override
    public List<HealthCheckResult> getHistory(@Nonnull String name) {
        Preconditions.checkNotNull(name, "Name must not be null");
        Preconditions.checkArgument(!name.trim().isEmpty(), "Name must not be empty");

        LinkedBlockingDeque<HealthCheckResult> historyQueue = this.historyMap.get(name);

        return historyQueue != null
                ? List.copyOf(historyQueue)
                : Collections.emptyList();
    }

    @Override
    public Set<HealthCheckResult> filterHistory(@Nonnull HealthCheckFilter healthCheckFilter) {
        Preconditions.checkNotNull(healthCheckFilter, "Health check filter must not be null");

        return this.historyMap.entrySet()
                .stream()
                .flatMap(entry -> entry.getValue().stream()
                        .map(result -> createContext(entry.getKey(), result)))
                .filter(healthCheckFilter::matches)
                .map(HealthCheckFilterContext::healthCheckResult)
                .collect(Collectors.toSet());
    }

    @Override
    public void clearHistory() {
        this.historyMap.clear();
    }

    private HealthCheckFilterContext createContext(String name, HealthCheckResult result) {
        return this.healthCheckRegistry.getHealthCheck(name)
                .map(hc -> new HealthCheckFilterContext(name, hc, result, hc.getTags()))
                .orElseGet(() -> new HealthCheckFilterContext(name, null, result, null)); // if not hc then must be
                                                                                          // getting history of
                                                                                          // aggregator
    }

}
