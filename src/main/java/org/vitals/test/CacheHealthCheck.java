package org.vitals.test;

import org.vitals.core.AbstractHealthCheck;

import java.util.Random;
import java.util.concurrent.TimeUnit;

public class CacheHealthCheck extends AbstractHealthCheck {

    public CacheHealthCheck(String name, String... tags) {
        super(name, tags);
    }

    private final Random random = new Random();

    @Override
    public HealthCheckResult check() throws Exception {

        try {
            int delay = random.nextInt(5) + 1;
            TimeUnit.SECONDS.sleep(delay);

            boolean isHealthy = random.nextBoolean();
            if (isHealthy) {
                return HealthCheckResult.builder()
                        .status(HealthStatus.HEALTHY)
                        .message("Cache is healthy")
                        .build();
            } else {
                return HealthCheckResult.builder()
                        .status(HealthStatus.UNHEALTHY)
                        .message("Cache is unhealthy")
                        .build();
            }
        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.UNHEALTHY)
                    .message("Unexpected error: " + e.getMessage())
                    .error(e)
                    .build();
        }

    }

}
