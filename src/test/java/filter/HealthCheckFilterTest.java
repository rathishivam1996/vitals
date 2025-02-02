package filter;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.vitals.core.HealthCheck;
import org.vitals.core.filter.HealthCheckFilter;
import org.vitals.core.filter.HealthCheckFilterContext;
import org.vitals.core.filter.HealthCheckFilters;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HealthCheckFilterTest {

    @Nested
    class Matches {
        @Test
        void matchesShouldReturnTrueForAllFilter() {
            HealthCheckFilter filter = HealthCheckFilter.ALL;
            HealthCheckFilterContext context = new HealthCheckFilterContext("testCheck", null, null,
                    Collections.emptySet());
            assertTrue(filter.matches(context));
        }

        @Test
        void matchesShouldRespectCustomFilterLogic() {
            HealthCheckFilter filter = context -> context.healthCheckName().startsWith("test");
            HealthCheckFilterContext context = new HealthCheckFilterContext("testCheck", null, null,
                    Collections.emptySet());
            assertTrue(filter.matches(context));

            HealthCheckFilterContext nonMatchingContext = new HealthCheckFilterContext("prodCheck", null, null,
                    Collections.emptySet());
            assertFalse(filter.matches(nonMatchingContext));
        }
    }

    @Nested
    class AnyOf {
        @Test
        void anyOfShouldMatchIfAnyFilterMatches() {
            HealthCheckFilter filter = HealthCheckFilter.anyOf(context -> context.healthCheckName().equals("Check1"),
                    context -> context.healthCheckName().equals("Check2"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, null,
                    Collections.emptySet());
            assertTrue(filter.matches(context));
        }

        @Test
        void anyOfShouldNotMatchIfNoFilterMatches() {
            HealthCheckFilter filter = HealthCheckFilter.anyOf(context -> context.healthCheckName().equals("Check1"),
                    context -> context.healthCheckName().equals("Check2"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check3", null, null,
                    Collections.emptySet());
            assertFalse(filter.matches(context));
        }
    }

    @Nested
    class AllOf {
        @Test
        void allOfShouldMatchIfAllFiltersMatch() {
            HealthCheckFilter filter = HealthCheckFilter.allOf(context -> context.healthCheckName().startsWith("Check"),
                    context -> context.tags().contains("critical"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, null, Set.of("critical"));
            assertTrue(filter.matches(context));
        }

        @Test
        void allOfShouldNotMatchIfAnyFilterDoesNotMatch() {
            HealthCheckFilter filter = HealthCheckFilter.allOf(context -> context.healthCheckName().startsWith("Check"),
                    context -> context.tags().contains("critical"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, null, Set.of("info"));
            assertFalse(filter.matches(context));
        }
    }

    @Nested
    class ByFilters {
        @Test
        void byNameShouldMatchSpecificName() {
            HealthCheckFilter filter = HealthCheckFilters.byName("DatabaseCheck");
            HealthCheckFilterContext context = new HealthCheckFilterContext("DatabaseCheck", null, null,
                    Collections.emptySet());
            assertTrue(filter.matches(context));

            HealthCheckFilterContext nonMatchingContext = new HealthCheckFilterContext("MemoryCheck", null, null,
                    Collections.emptySet());
            assertFalse(filter.matches(nonMatchingContext));
        }

        @Test
        void byStatusShouldMatchSpecificHealthStatus() {
            HealthCheckFilter filter = HealthCheckFilters.byStatus(HealthCheck.HealthStatus.HEALTHY);
            HealthCheck.HealthCheckResult result = HealthCheck.HealthCheckResult.builder()
                    .status(HealthCheck.HealthStatus.HEALTHY)
                    .message("All good")
                    .error(null)
                    .timeToLive(Duration.of(1L, ChronoUnit.HOURS))
                    .addData("key", new byte[]{1, 2, 3})
                    .build();

            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, result,
                    Collections.emptySet());
            assertTrue(filter.matches(context));
        }

        @Test
        void byAnyTagShouldMatchIfAnyTagExists() {
            HealthCheckFilter filter = HealthCheckFilters.byAnyTag(Set.of("critical", "urgent"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, null,
                    Set.of("info", "critical"));
            assertTrue(filter.matches(context));
        }

        @Test
        void byAllTagsShouldMatchOnlyIfAllTagsExist() {
            HealthCheckFilter filter = HealthCheckFilters.byAllTags(Set.of("critical", "urgent"));
            HealthCheckFilterContext context = new HealthCheckFilterContext("Check1", null, null,
                    Set.of("critical", "urgent", "info"));
            assertTrue(filter.matches(context));

            HealthCheckFilterContext nonMatchingContext = new HealthCheckFilterContext("Check1", null, null,
                    Set.of("critical", "info"));
            assertFalse(filter.matches(nonMatchingContext));
        }
    }

    @Nested
    class FilterBuilder {
        @Test
        void filterBuilderShouldCombineFiltersCorrectly() {
            HealthCheckFilter filter = new HealthCheckFilters.HealthCheckFilterBuilder().withName("DatabaseCheck")
                    .withStatus(HealthCheck.HealthStatus.HEALTHY)
                    .withAnyTag(Set.of("critical", "urgent"))
                    .build();

            HealthCheck.HealthCheckResult result = HealthCheck.HealthCheckResult.builder()
                    .status(HealthCheck.HealthStatus.HEALTHY)
                    .message("All good")
                    .error(null)
                    .timeToLive(Duration.of(1L, ChronoUnit.HOURS))
                    .addData("key", new byte[]{1, 2, 3})
                    .build();
            HealthCheckFilterContext context = new HealthCheckFilterContext("DatabaseCheck", null, result,
                    Set.of("critical"));

            assertTrue(filter.matches(context));
        }
    }
}
