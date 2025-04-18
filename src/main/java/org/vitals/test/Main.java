package org.vitals.test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheckManager;
import org.vitals.core.aggregator.MostSevereStateAggregator;
import org.vitals.core.aggregator.WeightedScoringAggregator;
import org.vitals.core.event.HealthCheckCheckedEvent;
import org.vitals.core.event.HealthCheckFailedEvent;
import org.vitals.core.filter.HealthCheckFilter;

public class Main {
        public static void main(String[] args) throws InterruptedException {
                HealthCheckManager healthCheckManager = new HealthCheckManager();

                GitHubAPIHealthCheck apiHealthCheck = new GitHubAPIHealthCheck("API Health Check", "API", "Test-Tag-1",
                        "Test-Tag-2");
                DatabaseHealthCheck databaseHealthCheck = new DatabaseHealthCheck("Database Health Check", "DB1",
                        "Test-Tag-1");
                MostSevereStateAggregator mostSevereStateAggregator = new MostSevereStateAggregator();
                WeightedScoringAggregator weightedScoringAggregator = new WeightedScoringAggregator();

                HealthCheckFilter gitHubNameFilter = HealthCheckFilter.and((context -> context.tags().contains("API")),
                        context -> context.tags().contains("github"));

                HealthCheckFilter tagFilter = HealthCheckFilter.or((context) -> context.tags().contains("Test-Tag-2"),
                        (context -> context.tags().contains("Test-Tag-2")));

                healthCheckManager.addListener(new ConsoleStatusUpdateListener(), tagFilter,
                        Set.of(HealthCheckCheckedEvent.class, HealthCheckFailedEvent.class));
                healthCheckManager.registerHealthCheck(apiHealthCheck);
                healthCheckManager.registerHealthCheck(databaseHealthCheck);
                healthCheckManager.registerAggregator(mostSevereStateAggregator);
                healthCheckManager.registerAggregator(weightedScoringAggregator);

                Set<CompletableFuture<HealthCheck.HealthCheckResult>> futures = healthCheckManager.executeAsync(tagFilter);

                while (true) {
                }

        }

}