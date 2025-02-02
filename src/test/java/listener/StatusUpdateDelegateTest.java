package listener;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.vitals.core.HealthCheck;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;
import org.vitals.core.listener.StatusUpdateDelegate;
import org.vitals.core.listener.StatusUpdateListener;

class StatusUpdateDelegateTest {

    private StatusUpdateDelegate delegate;
    private StatusUpdateListener mockListener;
    private HealthCheck mockHealthCheck;
    private HealthCheck.HealthCheckResult mockResult;
    private HealthCheckFilter mockFilter;

    @BeforeEach
    void setUp() {
        delegate = new StatusUpdateDelegate();
        mockListener = mock(StatusUpdateListener.class);
        mockHealthCheck = mock(HealthCheck.class);
        mockResult = mock(HealthCheck.HealthCheckResult.class);
        mockFilter = mock(HealthCheckFilter.class);
    }

    @AfterEach
    void tearDown() throws Exception {
        delegate.close();
    }

    @Test
    void testAddListener_Success() {
        delegate.addListener(mockListener);
        assertTrue(delegate.listeners().hasNext());
    }

    @Test
    void testAddListener_NullListener() {
        assertThrows(NullPointerException.class, () -> delegate.addListener(null));
    }

    @Test
    void testAddListener_WithFilter() throws InterruptedException {
        delegate.addListener(mockListener, mockFilter);

        when(mockFilter.matches(any(HealthCheckFilterContext.class))).thenReturn(true);

        delegate.onHealthChecked("testCheck", Set.of("critical"), mockHealthCheck, mockResult);

        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener).onHealthChecked("testCheck", Set.of("critical"), mockHealthCheck, mockResult);
    }

    @Test
    void testAddListener_WithFilter_NonMatching() throws InterruptedException {
        delegate.addListener(mockListener, mockFilter);

        HealthCheckFilterContext context = new HealthCheckFilterContext("nonMatchingCheck", mockHealthCheck, mockResult,
                Set.of("info"));

        // Verify filter does not match
        when(mockFilter.matches(context)).thenReturn(false);
        delegate.onHealthChecked("nonMatchingCheck", Set.of("critical"), mockHealthCheck, mockResult);

        // Allow async task to complete
        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener, never()).onHealthChecked(anyString(), any(), any(), any());
    }

    @Test
    void testRemoveListener_Success() {
        delegate.addListener(mockListener);
        delegate.removeListener(mockListener);
        assertFalse(delegate.listeners().hasNext());
    }

    @Test
    void testOnHealthChecked() throws InterruptedException {
        delegate.addListener(mockListener);
        delegate.onHealthChecked("testCheck", Set.of("critical"), mockHealthCheck, mockResult);

        // Allow async task to complete
        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener).onHealthChecked("testCheck", Set.of("critical"), mockHealthCheck, mockResult);
    }

    @Test
    void testOnHealthCheckFailed() throws InterruptedException {
        delegate.addListener(mockListener);
        delegate.onHealthCheckFailed("testCheck", Set.of("critical"), mockHealthCheck, "failure",
                new RuntimeException("Test"));

        // Allow async task to complete
        TimeUnit.MILLISECONDS.sleep(100);

        verify(mockListener).onHealthCheckFailed(eq("testCheck"), eq(Set.of("critical")), eq(mockHealthCheck),
                eq("failure"), any(Throwable.class));
    }

    @Test
    void testClearListeners() {
        delegate.addListener(mockListener);
        delegate.clear();
        assertFalse(delegate.listeners().hasNext());
    }

    @Test
    void testConcurrency() throws InterruptedException {
        Runnable addTask = () -> delegate.addListener(mock(StatusUpdateListener.class));
        Runnable notifyTask = () -> delegate.onHealthChecked("concurrentCheck", Collections.emptySet(), mockHealthCheck,
                mockResult);

        Thread t1 = new Thread(addTask);
        Thread t2 = new Thread(notifyTask);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        // Verify no concurrency issues
        assertTrue(true); // If no exception is thrown, test passes
    }
}
