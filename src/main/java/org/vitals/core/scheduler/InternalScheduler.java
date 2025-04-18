package org.vitals.core.scheduler;

import jakarta.annotation.Nonnull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public final class InternalScheduler {
    private static final Logger LOGGER = LoggerFactory.getLogger(InternalScheduler.class);
    private static final String TASK_REJECTED_MESSAGE = "Task rejected: scheduler state = ";
    private static final int DEFAULT_THREAD_POOL_SIZE = Math.max(2, Runtime.getRuntime().availableProcessors());
    private final AtomicReference<State> state;
    private final SchedulerConfig config;
    private volatile ScheduledExecutorService scheduler;
    private InternalScheduler(SchedulerConfig config) {
        this.config = config;
        this.state = new AtomicReference<>(State.INITIALIZED);
        initialize();
    }

    /**
     * Returns the singleton instance of InternalScheduler.
     *
     * @return the singleton instance.
     */
    public static InternalScheduler getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Creates a new instance of InternalScheduler with the given configuration.
     *
     * @param config the scheduler configuration.
     * @return a new instance of InternalScheduler.
     */
    public static InternalScheduler newInstance(SchedulerConfig config) {
        return new InternalScheduler(config);
    }

    private static boolean isVirtualThreadsSupported() {
//        try {
//            Thread.ofVirtual(); // If this call succeeds, virtual threads are supported.
//            return true;
//        } catch (Exception ignored) {
//            return false;
//        }
        return false;
    }

    /**
     * Returns the underlying {@link ScheduledExecutorService} instance.
     *
     * <p>
     * <b>Caution:</b> While access to the underlying executor service is provided,
     * it is strongly recommended to use the scheduling and submission methods
     * provided by {@link InternalScheduler} to ensure proper state management
     * and lifecycle handling. Directly manipulating the
     * {@code ScheduledExecutorService}
     * might lead to unexpected behavior if not used carefully in conjunction with
     * the {@code InternalScheduler}'s lifecycle. Specifically, do not call
     * {@code shutdown()} or {@code shutdownNow()} on the returned instance, as
     * shutdown should
     * be managed through the {@link InternalScheduler#shutdown()} method.
     *
     * @return the underlying {@code ScheduledExecutorService} instance, or
     * {@code null}
     * if the scheduler is not in {@link State#RUNNING} state or has not
     * been initialized yet.
     * @throws IllegalStateException if the scheduler is not in
     *                               {@link State#RUNNING} state.
     */

    public ScheduledExecutorService getExecutorService() {
        State currentState = state.get();
        if (currentState != State.RUNNING) {
            throw new IllegalStateException("Scheduler is not in RUNNING state: " + currentState);
        }
        return getSchedulerInstance(); // Reuse getSchedulerInstance to ensure initialization and RUNNING state
    }

    /**
     * Initializes the scheduler if it is not already initialized or has been shut
     * down.
     */
    private synchronized void initialize() {
        if (scheduler == null || scheduler.isShutdown()) {
            boolean supportsVirtualThreads = isVirtualThreadsSupported();

            // Use an AtomicInteger for thread naming.
            AtomicInteger threadCount = new AtomicInteger(1);
            ThreadFactory threadFactory = task -> {
                Thread thread = new Thread(task);
                thread.setName(config.threadNamePrefix + "-" + threadCount.getAndIncrement());
                thread.setDaemon(true);
                return thread;
            };

            /*
            if (supportsVirtualThreads && config.preferVirtualThreads) {
                threadFactory = task -> Thread.ofVirtual()
                        .name(config.threadNamePrefix + "-" + threadCount.getAndIncrement())
                        .unstarted(task);
            } else {
                threadFactory = task -> {
                    Thread thread = new Thread(task);
                    thread.setName(config.threadNamePrefix + "-" + threadCount.getAndIncrement());
                    thread.setDaemon(true);
                    return thread;
                };
            }
             */
            ScheduledThreadPoolExecutor executor = null;
            try {
                executor = new ScheduledThreadPoolExecutor(config.poolSize, threadFactory);
                executor.setRemoveOnCancelPolicy(true);
            } catch (Exception e) {
                LOGGER.error("Scheduler initialization failed: {}: {}", e.getClass().getName(), e.getMessage());
                scheduler = null; // Ensure scheduler is null on failure
                return; // Important: Exit method on failure
            }
            scheduler = executor;
            state.set(State.RUNNING);
        }
    }

    /**
     * Shuts down the scheduler, waiting for termination up to the configured
     * timeout.
     */
    public void shutdown() {
        if (!state.compareAndSet(State.RUNNING, State.SHUTTING_DOWN)) {
            return;
        }

        synchronized (this) {
            try {
                if (scheduler != null) {
                    scheduler.shutdown();
                    // wait for tasks to finish execution
                    if (!scheduler.awaitTermination(config.shutdownTimeout.toMillis(), TimeUnit.MILLISECONDS)) {
                        scheduler.shutdownNow(); // if still not completed, try force shutdown
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                if (scheduler != null) {
                    scheduler.shutdownNow(); // try force shutdown
                }
            } finally {
                scheduler = null; // nothing we can do at this point
                state.set(State.SHUTDOWN);
            }
        }
    }

    /**
     * Restarts the scheduler by shutting it down and reinitializing.
     */
    public synchronized void restart() {
        shutdown();
        initialize();
    }

    public State getState() {
        return state.get();
    }

    /**
     * Checks whether virtual threads are supported.
     *
     * @return true if virtual threads are supported, false otherwise.
     */
    public boolean isVirtualThreadsEnabled() {
        return config.preferVirtualThreads && isVirtualThreadsSupported();
    }

    /**
     * Schedules a task to be executed after a specified delay.
     *
     * @param task  the task to be executed
     * @param delay the delay before the task is executed
     * @param unit  the time unit of the delay parameter
     * @return a ScheduledFuture representing pending completion of the task
     */
    public ScheduledFuture<?> schedule(Runnable task, long delay, TimeUnit unit) {
        final ScheduledExecutorService schedulerSnapshot = getSchedulerInstance();
        try {
            return schedulerSnapshot.schedule(task, delay, unit);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(TASK_REJECTED_MESSAGE + state.get(), e);
        }
    }

    /**
     * Schedules a task to be executed periodically with a fixed delay between the
     * end of one execution and the start of the next.
     *
     * @param task         the task to be executed
     * @param initialDelay the delay before the first execution
     * @param period       the period between successive executions
     * @param unit         the time unit of the initialDelay and period parameters
     * @return a ScheduledFuture representing pending completion of the task
     */
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable task, long initialDelay, long period, TimeUnit unit) {
        final ScheduledExecutorService schedulerSnapshot = getSchedulerInstance();
        try {
            return schedulerSnapshot.scheduleWithFixedDelay(task, initialDelay, period, unit);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(TASK_REJECTED_MESSAGE + state.get(), e);
        }
    }

    /**
     * Executes a task immediately using the scheduler's thread pool.
     * If virtual threads are enabled, the task will run on a virtual thread.
     *
     * @param task the task to execute
     * @throws IllegalStateException if the scheduler is not in RUNNING state
     */
    public void execute(Runnable task) {
        final ScheduledExecutorService schedulerSnapshot = getSchedulerInstance();

        try {
            schedulerSnapshot.execute(task);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(TASK_REJECTED_MESSAGE + state.get(), e);
        }
    }

    /**
     * Submits a task for execution and returns a Future representing the task.
     * If virtual threads are enabled, the task will run on a virtual thread.
     *
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws IllegalStateException if the scheduler is not in RUNNING state
     */
    public Future<?> submit(Runnable task) {
        final ScheduledExecutorService schedulerSnapshot = getSchedulerInstance();
        try {
            return schedulerSnapshot.submit(task);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(TASK_REJECTED_MESSAGE + state.get(), e);
        }
    }

    /**
     * Submits a value-returning task for execution and returns a Future
     * representing the pending results.
     * If virtual threads are enabled, the task will run on a virtual thread.
     *
     * @param <T>  the type of the task's result
     * @param task the task to submit
     * @return a Future representing pending completion of the task
     * @throws IllegalStateException if the scheduler is not in RUNNING state
     */
    public <T> Future<T> submit(Callable<T> task) {
        final ScheduledExecutorService schedulerSnapshot = getSchedulerInstance();
        try {
            return schedulerSnapshot.submit(task);
        } catch (RejectedExecutionException e) {
            throw new IllegalStateException(TASK_REJECTED_MESSAGE + state.get(), e);
        }
    }

    private ScheduledExecutorService getSchedulerInstance() {
        State currentState = state.get();
        if (currentState != State.RUNNING) {
            throw new IllegalStateException("Scheduler is not in RUNNING state: " + currentState);
        }

        ScheduledExecutorService instance = scheduler;
        if (instance == null || instance.isShutdown()) {
            synchronized (this) {
                instance = scheduler;
                if (instance == null || instance.isShutdown()) {
                    initialize();
                    instance = scheduler;
                    if (instance == null) {
                        throw new IllegalStateException("Failed to initialize scheduler");
                    }
                }
            }
        }
        return instance;
    }

    /**
     * Checks if the scheduler is running.
     *
     * @return true if the scheduler is running, false otherwise
     */
    public synchronized boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }

    private enum State {
        INITIALIZED, RUNNING, SHUTTING_DOWN, SHUTDOWN
    }

    public static class SchedulerConfig {
        private final int poolSize;
        private final Duration shutdownTimeout;
        private final boolean preferVirtualThreads;
        private final String threadNamePrefix;

        private SchedulerConfig(Builder builder) {
            this.poolSize = builder.poolSize;
            this.shutdownTimeout = builder.shutdownTimeout;
            this.preferVirtualThreads = builder.preferVirtualThreads;
            this.threadNamePrefix = builder.threadNamePrefix;
        }

        public static class Builder {
            private int poolSize = DEFAULT_THREAD_POOL_SIZE;
            private Duration shutdownTimeout = Duration.ofSeconds(5);
            private boolean preferVirtualThreads = true;
            private String threadNamePrefix = "VitalsScheduler";

            public Builder withPoolSize(int poolSize) {
                this.poolSize = poolSize;
                return this;
            }

            public Builder withShutdownTimeout(Duration timeout) {
                this.shutdownTimeout = timeout;
                return this;
            }

            public Builder withVirtualThreads(boolean useVirtualThreads) {
                this.preferVirtualThreads = useVirtualThreads;
                return this;
            }

            public Builder withThreadNamePrefix(String prefix) {
                this.threadNamePrefix = prefix;
                return this;
            }

            public SchedulerConfig build() {
                return new SchedulerConfig(this);
            }
        }
    }

    private static class Holder {
        private static final InternalScheduler INSTANCE = new InternalScheduler(new SchedulerConfig.Builder().build());
    }

    /**
     * Custom Thread Factory to create and name threads.
     */
    @SuppressWarnings("unused")
    private static class VitalsThreadFactory implements ThreadFactory {
        private final String threadNamePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        VitalsThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(@Nonnull Runnable r) {
            Thread thread = new Thread(r, threadNamePrefix + "-Thread-" + threadNumber.getAndIncrement());
            thread.setDaemon(true); // Daemon threads do not block JVM shutdown
            return thread;
        }
    }

}
