package executor;

import org.junit.jupiter.api.Test;
import org.vitals.core.executor.strategy.ExecutionStrategy;
import org.vitals.core.executor.strategy.NoOpExecutionStrategy;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class NoOpExecutionStrategyTest {

    private final NoOpExecutionStrategy noOpExecutionStrategy = new NoOpExecutionStrategy();

    @Test
    void testExecuteWithStrategy_ReturnsResult() throws Exception {
        // Arrange
        String expectedResult = "Success";
        ExecutionStrategy.CheckedSupplier<CompletableFuture<String>> supplier = () -> CompletableFuture.completedFuture(expectedResult);

        // Act
        CompletableFuture<String> resultFuture = noOpExecutionStrategy.executeWithStrategy(supplier);

        // Assert
        assertNotNull(resultFuture);
        assertEquals(expectedResult, resultFuture.get());
    }

    @Test
    void testExecuteWithStrategy_ThrowsException() {
        // Arrange
        ExecutionStrategy.CheckedSupplier<CompletableFuture<String>> supplier = () -> {
            throw new RuntimeException("Test exception");
        };

        // Act & Assert
        Exception exception = assertThrows(RuntimeException.class, () -> {
            noOpExecutionStrategy.executeWithStrategy(supplier);
        });
        assertEquals("Test exception", exception.getMessage());
    }

    @Test
    void testExecuteWithStrategy_CompletesFutureSuccessfully() throws Exception {
        // Arrange
        ExecutionStrategy.CheckedSupplier<CompletableFuture<String>> supplier = () -> CompletableFuture.supplyAsync(() -> "Async Result");

        // Act
        CompletableFuture<String> resultFuture = noOpExecutionStrategy.executeWithStrategy(supplier);

        // Assert
        assertNotNull(resultFuture);
        assertEquals("Async Result", resultFuture.get());
    }

    @Test
    void testExecuteWithStrategy_PropagatesCheckedException() {
        // Arrange
        ExecutionStrategy.CheckedSupplier<CompletableFuture<String>> supplier = () -> {
            throw new Exception("Checked exception");
        };

        // Act & Assert
        Exception exception = assertThrows(Exception.class, () -> {
            noOpExecutionStrategy.executeWithStrategy(supplier);
        });
        assertEquals("Checked exception", exception.getMessage());
    }
}