// package history;

// import static org.junit.jupiter.api.Assertions.assertNotNull;
// import static org.junit.jupiter.api.Assertions.assertTrue;
// import static org.mockito.Mockito.mock;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Set;

// import org.example.core.HealthCheck;
// import org.example.core.HealthCheck.HealthCheckResult;
// import org.example.filter.HealthCheckFilter;
// import org.example.history.DefaultHealthCheckHistory;
// import org.example.listener.StatusUpdateDelegate;
// import org.junit.jupiter.api.Test;

// class DefaultHistoryTest {

//     @Test
//     void testConcurrentAddHistory() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Thread thread1 = new Thread(addTask);
//         Thread thread2 = new Thread(addTask);

//         thread1.start();
//         thread2.start();

//         thread1.join();
//         thread2.join();

//         List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//         assertTrue(historyList.size() <= 5); // Ensure max history size is respected
//     }

//     @Test
//     void testConcurrentGetHistoryAndAddHistory() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Runnable getTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//                 assertNotNull(historyList); // Ensure history is being read safely
//             }
//         };

//         Thread addThread = new Thread(addTask);
//         Thread getThread = new Thread(getTask);

//         addThread.start();
//         getThread.start();

//         addThread.join();
//         getThread.join();

//         List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//         assertTrue(historyList.size() <= 5); // Ensure max history size is respected
//     }

//     @Test
//     void testConcurrentFilterHistoryAndClearHistory() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);
//         history.addHistory(healthCheck, result);

//         Runnable filterTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 Set<HealthCheckResult> filteredResults = history.filterHistory(mock(HealthCheckFilter.class));
//                 assertNotNull(filteredResults); // Ensure filtered results are being retrieved
//             }
//         };

//         Runnable clearTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.clearHistory(); // Concurrently clear history
//             }
//         };

//         Thread filterThread = new Thread(filterTask);
//         Thread clearThread = new Thread(clearTask);

//         filterThread.start();
//         clearThread.start();

//         filterThread.join();
//         clearThread.join();
//     }

//     @Test
//     void testConcurrentAddHistoryAndFilterHistory() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Runnable filterTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 Set<HealthCheckResult> filteredResults = history.filterHistory(mock(HealthCheckFilter.class));
//                 assertNotNull(filteredResults); // Ensure filtering works while adding history
//             }
//         };

//         Thread addThread = new Thread(addTask);
//         Thread filterThread = new Thread(filterTask);

//         addThread.start();
//         filterThread.start();

//         addThread.join();
//         filterThread.join();
//     }

//     @Test
//     void testConcurrentAddHistoryForDifferentHealthChecks() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck1 = mock(HealthCheck.class);
//         HealthCheck healthCheck2 = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask1 = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck1, result);
//             }
//         };

//         Runnable addTask2 = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck2, result);
//             }
//         };

//         Thread thread1 = new Thread(addTask1);
//         Thread thread2 = new Thread(addTask2);

//         thread1.start();
//         thread2.start();

//         thread1.join();
//         thread2.join();

//         List<HealthCheckResult> historyList1 = history.getHistory(healthCheck1);
//         List<HealthCheckResult> historyList2 = history.getHistory(healthCheck2);

//         assertTrue(historyList1.size() <= 5); // Ensure max history size is respected
//         assertTrue(historyList2.size() <= 5); // Ensure max history size is respected
//     }

//     @Test
//     void testStressTestWithHighConcurrency() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, new StatusUpdateDelegate());
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Runnable filterTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.filterHistory(mock(HealthCheckFilter.class));
//             }
//         };

//         Runnable getTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.getHistory(healthCheck);
//             }
//         };

//         List<Thread> threads = new ArrayList<>();
//         for (int i = 0; i < 10; i++) {
//             threads.add(new Thread(addTask));
//             threads.add(new Thread(filterTask));
//             threads.add(new Thread(getTask));
//         }

//         for (Thread thread : threads) {
//             thread.start();
//         }

//         for (Thread thread : threads) {
//             thread.join();
//         }
//     }

//     @Test
//     void testDataIntegrityAfterAddingAndRetrieving() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, mock(StatusUpdateDelegate.class));
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Runnable getTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//                 assertTrue(historyList.contains(result)); // Ensure the added data is in the result
//             }
//         };

//         Thread addThread = new Thread(addTask);
//         Thread getThread = new Thread(getTask);

//         addThread.start();
//         getThread.start();

//         addThread.join();
//         getThread.join();
//     }

//     @Test
//     void testClearHistoryEmptiesHistory() {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, mock(StatusUpdateDelegate.class));
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         history.addHistory(healthCheck, result);

//         // Clear history
//         history.clearHistory();

//         // Ensure history is empty after clearing
//         List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//         assertTrue(historyList.isEmpty(), "History should be empty after clearing");
//     }

//     @Test
//     void testGetHistoryWhileAdding() throws InterruptedException {
//         DefaultHealthCheckHistory history = new DefaultHealthCheckHistory(5, mock(StatusUpdateDelegate.class));
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         HealthCheckResult result = mock(HealthCheckResult.class);

//         Runnable addTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 history.addHistory(healthCheck, result);
//             }
//         };

//         Runnable getTask = () -> {
//             for (int i = 0; i < 100; i++) {
//                 List<HealthCheckResult> historyList = history.getHistory(healthCheck);
//                 assertTrue(historyList.size() <= 5);
//                 assertTrue(historyList.contains(result));
//             }
//         };

//         Thread addThread = new Thread(addTask);
//         Thread getThread = new Thread(getTask);

//         addThread.start();
//         getThread.start();

//         addThread.join();
//         getThread.join();
//     }

// }
