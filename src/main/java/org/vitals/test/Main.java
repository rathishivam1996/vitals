package org.vitals.test;

import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.vitals.core.aggregator.MostSevereStateAggregator;
import org.vitals.core.aggregator.WeightedScoringAggregator;
import org.vitals.core.HealthCheck;
import org.vitals.core.HealthCheckManager;

public class Main {
        public static void main(String[] args) throws InterruptedException {

                CacheHealthCheck cacheHealthCheck = new CacheHealthCheck("cache", "cachi1", "cache2", "tag1", "tag2");

                SampleHealthCheck sampleHealthCheck = new SampleHealthCheck("sample",
                                "sample1", "sample2", "tag1", "tag3");

                DatabaseHealthCheck databaseHealthCheck = new DatabaseHealthCheck("database",
                                "database1", "database2", "tag2");

                APIHealthCheck apiHealthCheck = new APIHealthCheck("api",
                                "api1", "api2", "tag3");

                MostSevereStateAggregator mostSevereStateAggregator = new MostSevereStateAggregator();
                WeightedScoringAggregator weightedScoringAggregator = new WeightedScoringAggregator();

                HealthCheckManager healthCheckManager = new HealthCheckManager();

                healthCheckManager.registerHealthCheck(cacheHealthCheck);
                healthCheckManager.registerHealthCheck(sampleHealthCheck);
                healthCheckManager.registerHealthCheck(databaseHealthCheck);
                healthCheckManager.registerHealthCheck(apiHealthCheck);
                healthCheckManager.registerAggregator(mostSevereStateAggregator);
                healthCheckManager.registerAggregator(weightedScoringAggregator);

                Set<CompletableFuture<HealthCheck.HealthCheckResult>> tag1Res = healthCheckManager
                                .executeAsync((context -> context.tags().contains("tag1")));
                CompletableFuture<HealthCheck.HealthCheckResult> databaseRes = healthCheckManager
                                .executeAsync("database");

                // log the results usinng slf4j logger
                tag1Res.forEach(result -> result.thenAccept(r -> System.out.println("tag1Res: " + r)));
                databaseRes.thenAccept(r -> System.out.println("databaseRes: " + r));

                while(true) {

                }

        }

}