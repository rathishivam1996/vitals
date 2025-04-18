package org.vitals.test;

import org.vitals.core.AbstractHealthCheck;
import org.vitals.core.annotation.AsyncHealthCheck;

import java.util.Random;
import java.util.concurrent.TimeUnit;

@AsyncHealthCheck(period = 5, initialDelay = 0)
public class DatabaseHealthCheck extends AbstractHealthCheck {

    public DatabaseHealthCheck(String name, String... tags) {
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
                return HealthCheckResult.builder().status(HealthStatus.HEALTHY).message("Database is healthy").build();
            } else {
                return HealthCheckResult.builder()
                        .status(HealthStatus.UNHEALTHY)
                        .message("Database is unhealthy")
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
