package org.vitals.core.scheduler;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinWorkerThread;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class VitalsScheduler {
    private static final int DEFAULT_SCHEDULER_THREADS = 2;
    private static final int DEFAULT_PARALLELISM = Math.max(2, Runtime.getRuntime().availableProcessors() - 1);
    private static volatile ScheduledExecutorService scheduler;
    private static volatile ExecutorService forkJoinPool;

    private VitalsScheduler() {
        initialize();
    }

    private static class Holder {
        private static final VitalsScheduler INSTANCE = new VitalsScheduler();
    }

    /**
     * Returns the singleton instance of VitalsScheduler.
     *
     * @return the singleton instance of VitalsScheduler
     */
    public static VitalsScheduler getInstance() {
        return Holder.INSTANCE;
    }

    /**
     * Creates a new instance with support for virtual threads.
     *
     * @return a new instance of VitalsScheduler
     */
    // public static VitalsScheduler newInstance() {
    //     return new VitalsScheduler();
    // }

    /**
     * Initializes the scheduler and forkJoinPool if they are not already
     * initialized or have been shut down.
     */
    private synchronized void initialize() {

        if (scheduler == null || scheduler.isShutdown()) {
            ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(DEFAULT_SCHEDULER_THREADS,
                    new VitalsThreadFactory("VitalsScheduler"));
            executor.setRemoveOnCancelPolicy(true);
            scheduler = executor;
        }

        if (forkJoinPool == null || forkJoinPool.isShutdown()) {
            forkJoinPool = new ForkJoinPool(
                    DEFAULT_PARALLELISM,
                    new VitalsForkJoinWorkerThreadFactory("VitalsForkJoin"),
                    null,
                    true);
        }

    }

    /**
     * Shuts down the scheduler and forkJoinPool, waiting for their termination.
     */
    public synchronized void shutdown() {
        try {
            if (scheduler != null) {
                scheduler.shutdown();
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            }
            if (forkJoinPool != null) {
                forkJoinPool.shutdown();
                if (forkJoinPool.awaitTermination(5, TimeUnit.SECONDS)) {
                    forkJoinPool.shutdownNow();
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            scheduler = null;
            forkJoinPool = null;
        }
    }

    /**
     * Restarts the scheduler and forkJoinPool by shutting them down and
     * reinitializing.
     */
    public synchronized void restart() {
        shutdown();
        initialize();
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
        synchronized (VitalsScheduler.class) {
            if (scheduler == null || scheduler.isShutdown()) {
                throw new IllegalStateException("Scheduler is not initialized or has been shut down.");
            }
        }
        return scheduler.schedule(() -> forkJoinPool.submit(task), delay, unit);
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
        synchronized (VitalsScheduler.class) {
            if (scheduler == null || scheduler.isShutdown()) {
                throw new IllegalStateException("Scheduler is not initialized or has been shut down.");
            }
        }
        return scheduler.scheduleAtFixedRate(() -> forkJoinPool.submit(task), initialDelay, period, unit);
    }

    /**
     * Checks if the scheduler is running.
     *
     * @return true if the scheduler is running, false otherwise
     */
    public synchronized boolean isRunning() {
        return scheduler != null && !scheduler.isShutdown();
    }

    /**
     * Executes a task immediately using the ForkJoinPool.
     *
     * @param task the task to be executed
     */
    public void execute(Runnable task) {
        synchronized (VitalsScheduler.class) {
            if (forkJoinPool == null || forkJoinPool.isShutdown()) {
                throw new IllegalStateException("ForkJoinPool is not initialized or has been shut down.");
            }
        }
        forkJoinPool.execute(task);
    }

    /**
     * Custom Thread Factory to create and name threads.
     */
    private static class VitalsThreadFactory implements ThreadFactory {
        private final String threadNamePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        VitalsThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, threadNamePrefix + "-Thread-" + threadNumber.getAndIncrement());
            thread.setDaemon(true); // Daemon threads do not block JVM shutdown
            return thread;
        }
    }

    /**
     * Custom ForkJoinWorkerThreadFactory to name threads in ForkJoinPool.
     */
    private static class VitalsForkJoinWorkerThreadFactory implements ForkJoinPool.ForkJoinWorkerThreadFactory {
        private final String threadNamePrefix;
        private final AtomicInteger threadNumber = new AtomicInteger(1);

        VitalsForkJoinWorkerThreadFactory(String threadNamePrefix) {
            this.threadNamePrefix = threadNamePrefix;
        }

        @Override
        public ForkJoinWorkerThread newThread(ForkJoinPool pool) {
            ForkJoinWorkerThread thread = ForkJoinPool.defaultForkJoinWorkerThreadFactory.newThread(pool);
            thread.setName(threadNamePrefix + "-Thread-" + threadNumber.getAndIncrement());
            return thread;
        }
    }

}
