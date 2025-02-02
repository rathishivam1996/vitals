package org.vitals.core;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vitals.core.aggregator.HealthResultAggregator;
import org.vitals.core.annotation.AsyncHealthCheck;
import org.vitals.core.HealthCheck.HealthCheckResult;
import org.vitals.core.executor.DefaultHealthCheckExecutor;
import org.vitals.core.executor.HealthCheckExecutor;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.history.DefaultHealthCheckHistory;
import org.vitals.core.history.HealthCheckHistory;
import org.vitals.core.listener.StatusUpdateDelegate;
import org.vitals.core.listener.StatusUpdateListener;
import org.vitals.core.registry.DefaultHealthCheckRegistry;
import org.vitals.core.registry.HealthCheckRegistry;
import org.vitals.core.scheduler.DefaultHealthCheckScheduler;
import org.vitals.core.scheduler.HealthCheckScheduler;
import org.vitals.core.scheduler.VitalsScheduler;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

public class HealthCheckManager
        implements StatusUpdateListener, HealthCheckRegistry, HealthCheckExecutor, HealthCheckScheduler,
        HealthCheckHistory {

    private final HealthCheckRegistry healthCheckRegistry;
    private final HealthCheckExecutor healthCheckExecutor;
    private final DefaultHealthCheckHistory defaultHealthCheckHistory;
    private final HealthCheckScheduler scheduler;

    @SuppressWarnings("unused")
    private static final ScheduledThreadPoolExecutor sharedExecutor;
    private final VitalsScheduler vitalsScheduler;

    private static final Logger logger = LoggerFactory.getLogger(HealthCheckManager.class);

    static {
        sharedExecutor = new ScheduledThreadPoolExecutor(
                10, // Core pool size
                new ThreadFactory() {
                    private final AtomicInteger threadCounter = new AtomicInteger(1);

                    @Override
                    public Thread newThread(Runnable r) {
                        return new Thread(r, "HealthCheck-" + threadCounter.getAndIncrement());
                    }
                });
    }

    public HealthCheckManager() {
        this.vitalsScheduler = VitalsScheduler.getInstance();
        StatusUpdateDelegate statusUpdateDelegate = new StatusUpdateDelegate(this.vitalsScheduler);
        this.healthCheckRegistry = new DefaultHealthCheckRegistry(statusUpdateDelegate);

        this.defaultHealthCheckHistory = new DefaultHealthCheckHistory(5, statusUpdateDelegate, healthCheckRegistry);
        this.healthCheckExecutor = new DefaultHealthCheckExecutor(
                this.healthCheckRegistry, statusUpdateDelegate, defaultHealthCheckHistory, this.vitalsScheduler);

        this.scheduler = new DefaultHealthCheckScheduler(this.healthCheckExecutor, this.vitalsScheduler);
        statusUpdateDelegate.addListener(this);
    }

    private void scheduleHealthCheck(@Nonnull String healthCheckName, @Nonnull AsyncHealthCheck asyncConfig) {
        long initialDelay = asyncConfig.initialDelay();
        long period = asyncConfig.period();
        TimeUnit unit = asyncConfig.unit();

        @SuppressWarnings("unused")
        AsyncHealthCheck.ScheduleType scheduleType = asyncConfig.scheduleType();

        this.schedule(healthCheckName, initialDelay, period, unit);
    }

    // Listener Management
    @Override
    public void onHealthCheckAdded(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck) {
        AsyncHealthCheck asyncConfig = healthCheck.getClass().getAnnotation(AsyncHealthCheck.class);
        if (asyncConfig != null && !this.isScheduled(name)) {
            this.scheduleHealthCheck(name, asyncConfig);
        }
    }

    @Override
    public void onHealthCheckRemoved(@Nonnull String name, @Nonnull Set<String> tags,
            @Nonnull HealthCheck healthCheck) {

        logger.info("Health check {} removed.", name);

        if (this.scheduler.isScheduled(name)) {
            this.scheduler.cancelScheduledHealthCheck(name);
        }
    }

    @Override
    public void onHealthChecked(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nonnull HealthCheck.HealthCheckResult healthCheckResult) {
        logger.info("Health check [ {} ] executed. Result: {}", name, healthCheckResult);
    }

    @Override
    public void onHealthCheckFailed(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable String message,
            @Nonnull Throwable throwable) {

        logger.error("Health check [ {} ] failed. Error: {}", name, message);
    }

    @Override
    public void onChanged(@Nonnull String name, @Nonnull Set<String> tags, @Nonnull HealthCheck healthCheck,
            @Nullable HealthCheck.HealthCheckResult original,
            @Nonnull HealthCheck.HealthCheckResult updated) {
        logger.info("Health check [ {} ] status changed from {} to {}",
                (original != null ? original.getStatus() : "UNKNOWN"), updated.getStatus());
    }

    @Override
    public void onHealthResultAggregated(@Nonnull String aggregatorName, @Nonnull HealthCheckResult aggregatedResult) {
        logger.info("Aggregator [ {} ] executed. Result: {}", aggregatorName, aggregatedResult.getStatus());
    }

    @Override
    public void onAggregatedResultChanged(@Nonnull String aggregatorName,
            @Nullable HealthCheckResult previousAggregated, @Nonnull HealthCheckResult updatedAggregated) {
        logger.info("Aggregator check [ {} ] status changed from {} to {}",
                (previousAggregated != null ? previousAggregated.getStatus() : "UNKNOWN"),
                updatedAggregated.getStatus());
    }

    // Executor Management
    @Override
    public CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull String healthCheckName) {
        return this.healthCheckExecutor.executeAsync(healthCheckName);
    }

    @Override
    public CompletableFuture<HealthCheck.HealthCheckResult> executeAsync(@Nonnull HealthCheck healthCheck) {
        return this.healthCheckExecutor.executeAsync(healthCheck);
    }

    @Override
    public Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAsync(@Nonnull HealthCheckFilter filter) {
        return this.healthCheckExecutor.executeAsync(filter);
    }

    @Override
    public Set<CompletableFuture<HealthCheck.HealthCheckResult>> executeAll() {
        return this.healthCheckExecutor.executeAll();
    }

    // Registry Management
    @Override
    public boolean registerHealthCheck(@Nonnull HealthCheck healthCheck) {
        return this.healthCheckRegistry.registerHealthCheck(healthCheck);
    }

    @Override
    public boolean isHealthCheckRegistered(@Nonnull String name) {
        return this.healthCheckRegistry.isHealthCheckRegistered(name);
    }

    @Override
    public Optional<HealthCheck> unregisterHealthCheck(@Nonnull String name) {
        return this.healthCheckRegistry.unregisterHealthCheck(name);
    }

    @Override
    public Optional<Set<HealthCheck>> unregisterHealthCheck(@Nonnull HealthCheckFilter filter) {
        return this.healthCheckRegistry.unregisterHealthCheck(filter);
    }

    @Override
    public Optional<HealthCheck> getHealthCheck(@Nonnull String name) {
        return this.healthCheckRegistry.getHealthCheck(name);
    }

    @Override
    public Optional<HealthCheck> unregisterHealthCheck(@Nonnull HealthCheck healthCheck) {
        return this.healthCheckRegistry.unregisterHealthCheck(healthCheck);
    }

    @Override
    public Set<HealthCheck> getAllHealthChecks() {
        return this.healthCheckRegistry.getAllHealthChecks();
    }

    @Override
    public Set<HealthCheck> filterHealthChecks(@Nonnull HealthCheckFilter filter) {
        return this.healthCheckRegistry.filterHealthChecks(filter);
    }

    @Override
    public void clearAllHealthChecks() {
        this.healthCheckRegistry.clearAllHealthChecks();
    }

    @Override
    public void addListener(@Nonnull StatusUpdateListener listener) {
        this.healthCheckRegistry.addListener(listener);
    }

    @Override
    public void removeListener(@Nonnull StatusUpdateListener listener) {
        this.healthCheckRegistry.removeListener(listener);
    }

    @Override
    public boolean registerAggregator(@Nonnull HealthResultAggregator aggregator) {
        return this.healthCheckRegistry.registerAggregator(aggregator);
    }

    @Override
    public boolean isAggregatorRegistered(@Nonnull String name) {
        return this.healthCheckRegistry.isAggregatorRegistered(name);
    }

    @Override
    public Optional<HealthResultAggregator> unregisterAggregator(@Nonnull String name) {
        return this.healthCheckRegistry.unregisterAggregator(name);
    }

    @Override
    public Optional<HealthResultAggregator> getAggregator(@Nonnull String name) {
        return this.healthCheckRegistry.getAggregator(name);
    }

    @Override
    public Set<HealthResultAggregator> getAllAggregators() {
        return this.healthCheckRegistry.getAllAggregators();
    }

    @Override
    public void clearAllAggregators() {
        this.healthCheckRegistry.clearAllAggregators();
    }

    // Scheduler Management
    @Override
    public void schedule(@Nonnull String healthCheckName, long initialDelay, long delay, @Nonnull TimeUnit timeUnit) {
        this.scheduler.schedule(healthCheckName, initialDelay, delay, timeUnit);
    }

    @Override
    public void scheduleWithCron(@Nonnull String healthCheckName, @Nonnull String cronExpression) {
        this.scheduler.scheduleWithCron(healthCheckName, cronExpression);
    }

    @Override
    public boolean isScheduled(@Nonnull String healthCheckName) {
        return this.scheduler.isScheduled(healthCheckName);
    }

    @Override
    public void cancelScheduledHealthCheck(@Nonnull String healthCheckName) {
        this.scheduler.cancelScheduledHealthCheck(healthCheckName);
    }

    // add history
    @Override
    public void addHistoryInternal(HealthCheck healthCheck, HealthCheckResult result) {
        throw new UnsupportedOperationException("Unimplemented method 'addHistory'");
    }

    @Override
    public List<HealthCheckResult> getHistory(@Nonnull String name) {
        return this.defaultHealthCheckHistory.getHistory(name);
    }

    @Override
    public Set<HealthCheckResult> filterHistory(@Nonnull HealthCheckFilter healthCheckFilter) {
        return this.defaultHealthCheckHistory.filterHistory(healthCheckFilter);
    }

    @Override
    public void clearHistory() {
        this.defaultHealthCheckHistory.clearHistory();
    }

}
