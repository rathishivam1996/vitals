// package registry;

// import org.example.core.HealthCheck;
// import org.example.filter.HealthCheckFilter;
// import org.example.listener.StatusUpdateDelegate;
// import org.example.listener.StatusUpdateListener;
// import org.example.registry.DefaultHealthCheckRegistry;
// import org.example.registry.HealthCheckRegistry;
// import org.junit.jupiter.api.BeforeEach;
// import org.junit.jupiter.api.Nested;
// import org.junit.jupiter.api.Test;

// import java.util.Set;

// import static org.junit.jupiter.api.Assertions.*;
// import static org.mockito.Mockito.mock;
// import static org.mockito.Mockito.when;


// class DefaultHealthCheckRegistryTest {

//     private HealthCheckRegistry registry;
//     private StatusUpdateDelegate statusUpdateDelegate;

//     @BeforeEach
//     void setUp() {
//         statusUpdateDelegate = new StatusUpdateDelegate();
//         registry = new DefaultHealthCheckRegistry(statusUpdateDelegate);
//     }

//     @Nested
//     class RegisterHealthCheckTests {

//         @Test
//         void shouldRegisterHealthCheckSuccessfully() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             boolean registered = registry.registerHealthCheck(healthCheck);

//             assertTrue(registered);
//             assertTrue(registry.isHealthCheckRegistered("check1"));
//         }

//         @Test
//         void shouldNotRegisterDuplicateHealthCheck() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             boolean registeredAgain = registry.registerHealthCheck(healthCheck);

//             assertFalse(registeredAgain);
//         }

//         @Test
//         void shouldNotAllowNullHealthCheck() {
//             assertThrows(NullPointerException.class, () -> registry.registerHealthCheck(null));
//         }
//     }

//     @Nested
//     class IsHealthCheckRegisteredTests {

//         @Test
//         void shouldReturnTrueForRegisteredHealthCheck() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             assertTrue(registry.isHealthCheckRegistered("check1"));
//         }

//         @Test
//         void shouldReturnFalseForUnregisteredHealthCheck() {
//             assertFalse(registry.isHealthCheckRegistered("nonexistent"));
//         }
//     }

//     @Nested
//     class UnregisterHealthCheckByNameTests {

//         @Test
//         void shouldUnregisterHealthCheckByName() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             HealthCheck unregistered = registry.unregisterHealthCheck("check1");

//             assertEquals(healthCheck, unregistered);
//             assertFalse(registry.isHealthCheckRegistered("check1"));
//         }

//         @Test
//         void shouldReturnNullIfHealthCheckNotFoundByName() {
//             HealthCheck unregistered = registry.unregisterHealthCheck("nonexistent");

//             assertNull(unregistered);
//         }
//     }

//     @Nested
//     class UnregisterHealthCheckByFilterTests {

//         @Test
//         void shouldUnregisterHealthCheckUsingFilter() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             HealthCheckFilter filter = context -> context.healthCheckName().equals("check1");
//             HealthCheck unregistered = registry.unregisterHealthCheck(filter);

//             assertEquals(healthCheck, unregistered);
//             assertFalse(registry.isHealthCheckRegistered("check1"));
//         }

//         @Test
//         void shouldReturnNullIfNoHealthCheckMatchesFilter() {
//             HealthCheckFilter filter = context -> context.healthCheckName().equals("nonexistent");

//             HealthCheck unregistered = registry.unregisterHealthCheck(filter);

//             assertNull(unregistered);
//         }
//     }

//     @Nested
//     class GetHealthCheckTests {

//         @Test
//         void shouldReturnHealthCheckByName() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             HealthCheck retrieved = registry.getHealthCheck("check1");

//             assertEquals(healthCheck, retrieved);
//         }

//         @Test
//         void shouldReturnNullIfHealthCheckNotFoundByName() {
//             assertNull(registry.getHealthCheck("nonexistent"));
//         }
//     }

//     @Nested
//     class GetAllHealthChecksTests {

//         @Test
//         void shouldReturnAllRegisteredHealthChecks() {
//             HealthCheck healthCheck1 = mockHealthCheck("check1");
//             HealthCheck healthCheck2 = mockHealthCheck("check2");

//             registry.registerHealthCheck(healthCheck1);
//             registry.registerHealthCheck(healthCheck2);

//             Set<HealthCheck> healthChecks = registry.getAllHealthChecks();

//             assertTrue(healthChecks.contains(healthCheck1));
//             assertTrue(healthChecks.contains(healthCheck2));
//         }

//         @Test
//         void shouldReturnEmptySetIfNoHealthChecksRegistered() {
//             Set<HealthCheck> healthChecks = registry.getAllHealthChecks();

//             assertTrue(healthChecks.isEmpty());
//         }
//     }

//     @Nested
//     class FilterHealthChecksTests {

//         @Test
//         void shouldFilterHealthChecksUsingProvidedFilter() {
//             HealthCheck healthCheck1 = mockHealthCheck("check1");
//             HealthCheck healthCheck2 = mockHealthCheck("check2");

//             registry.registerHealthCheck(healthCheck1);
//             registry.registerHealthCheck(healthCheck2);

//             HealthCheckFilter filter = context -> context.healthCheckName().equals("check1");
//             Set<HealthCheck> filtered = registry.filterHealthChecks(filter);

//             assertEquals(1, filtered.size());
//             assertTrue(filtered.contains(healthCheck1));
//         }

//         @Test
//         void shouldReturnEmptySetIfNoHealthChecksMatchFilter() {
//             HealthCheck healthCheck = mockHealthCheck("check1");
//             registry.registerHealthCheck(healthCheck);

//             HealthCheckFilter filter = context -> context.healthCheckName().equals("nonexistent");
//             Set<HealthCheck> filtered = registry.filterHealthChecks(filter);

//             assertTrue(filtered.isEmpty());
//         }
//     }

//     @Nested
//     class ListenerManagementTests {

//         @Test
//         void shouldAddAndRemoveListenerSuccessfully() {
//             StatusUpdateListener listener = mock(StatusUpdateListener.class);

//             registry.addListener(listener);

//             assertTrue(statusUpdateDelegate.isListenerRegistered(listener));

//             registry.removeListener(listener);

//             assertFalse(statusUpdateDelegate.isListenerRegistered(listener));
//         }
//     }

//     @Nested
//     class ClearAllHealthChecksTests {

//         @Test
//         void shouldClearAllRegisteredHealthChecks() {
//             HealthCheck healthCheck1 = mockHealthCheck("check1");
//             HealthCheck healthCheck2 = mockHealthCheck("check2");

//             registry.registerHealthCheck(healthCheck1);
//             registry.registerHealthCheck(healthCheck2);

//             registry.clearAllHealthChecks();

//             assertTrue(registry.getAllHealthChecks().isEmpty());
//         }
//     }

//     private HealthCheck mockHealthCheck(String name) {
//         HealthCheck healthCheck = mock(HealthCheck.class);
//         when(healthCheck.getName()).thenReturn(name);
//         return healthCheck;
//     }
// }