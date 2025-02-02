package executor;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.vitals.core.executor.strategy.ExecutionStrategy;
import org.vitals.core.executor.strategy.FailsafeExecutionStrategy;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.junit.jupiter.api.Assertions.*;

public class FailsafeExecutionStrategyTest {

    private FailsafeExecutionStrategy executionStrategy;

    @BeforeEach
    void setUp() {
        executionStrategy = new FailsafeExecutionStrategy(3, Duration.ofMillis(100), Duration.ofSeconds(1),
                Duration.ofMillis(50));
    }

    @Nested
    class ExecuteWithStrategyTests {

        @Test
        void testExecuteWithStrategy_Success() throws Exception {
            CompletableFuture<String> future = executionStrategy.executeWithStrategy(
                    () -> CompletableFuture.completedFuture("Success"));
            assertEquals("Success", future.get());
        }

        @Test
        void testExecuteWithStrategy_RetryAndFail() {
            assertThrows(ExecutionException.class, () -> {
                executionStrategy.executeWithStrategy(() -> {
                    throw new Exception("Failure");
                }).get();
            });
        }

        @Test
        void testExecuteWithStrategy_RetryAndSucceed() throws Exception {
            CompletableFuture<String> future = executionStrategy.executeWithStrategy(
                    new ExecutionStrategy.CheckedSupplier<CompletableFuture<String>>() {
                        private int attempt = 0;

                        @Override
                        public CompletableFuture<String> get() throws Exception {
                            if (attempt++ < 2) {
                                throw new Exception("Temporary failure");
                            }
                            return CompletableFuture.completedFuture("Success");
                        }
                    });
            assertEquals("Success", future.get());
        }
    }

    @Nested
    class ConstructorTests {
        @Test
        void testConstructor_InvalidMaxRetries() {
            assertThrows(IllegalArgumentException.class, () ->
                    new FailsafeExecutionStrategy(-1, Duration.ofMillis(100), Duration.ofSeconds(1),
                            Duration.ofMillis(50))
            );
        }

        @Test
        void testConstructor_InvalidInitialDelay() {
            assertThrows(IllegalArgumentException.class, () ->
                    new FailsafeExecutionStrategy(3, Duration.ofMillis(-100), Duration.ofSeconds(1),
                            Duration.ofMillis(50))
            );
        }

        @Test
        void testConstructor_InvalidMaxDelay() {
            assertThrows(IllegalArgumentException.class, () ->
                    new FailsafeExecutionStrategy(3, Duration.ofMillis(100), Duration.ofSeconds(-1),
                            Duration.ofMillis(50))
            );
        }

        @Test
        void testConstructor_ValidParameters() {
            assertDoesNotThrow(() -> {
                new FailsafeExecutionStrategy(3, Duration.ofMillis(100), Duration.ofSeconds(1), Duration.ofMillis(50));
            });
        }
    }
}