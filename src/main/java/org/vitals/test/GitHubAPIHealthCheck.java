package org.vitals.test;

import org.vitals.core.AbstractHealthCheck;
import org.vitals.core.annotation.AsyncHealthCheck;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@AsyncHealthCheck(period = 5, initialDelay = 0)
public class GitHubAPIHealthCheck extends AbstractHealthCheck {

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String RATE_LIMIT_ENDPOINT = "/rate_limit";

    private final HttpClient httpClient;

    public GitHubAPIHealthCheck(String name, String... tags) {
        super(name, tags);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10)) // Set a reasonable connection timeout
                .build();
    }

    @Override
    public HealthCheckResult check() throws Exception {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(GITHUB_API_URL + RATE_LIMIT_ENDPOINT))
                    .GET()
                    .timeout(Duration.ofSeconds(5)) // Set a reasonable request timeout
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                // Optionally, you could parse the JSON response to check for remaining rate limits
                // and report a DEGRADED status if the limit is very low.
                return HealthCheckResult.builder()
                        .status(HealthStatus.HEALTHY)
                        .message("GitHub API is accessible and responding.")
                        .addData("status_code", response.statusCode())
                        .build();
            } else if (response.statusCode() == 403) {
                return HealthCheckResult.builder()
                        .status(HealthStatus.DEGRADED)
                        .message(
                                "GitHub API returned a 403 (Forbidden). This might indicate rate limiting or other access issues.")
                        .addData("status_code", response.statusCode())
                        .build();
            } else {
                return HealthCheckResult.builder()
                        .status(HealthStatus.UNHEALTHY)
                        .message("GitHub API returned an unexpected status code: " + response.statusCode())
                        .addData("status_code", response.statusCode())
                        .addData("response_body", response.body()
                                .substring(0,
                                        Math.min(response.body().length(), 200)) + "...") // Log a snippet of the body
                        .build();
            }

        } catch (java.net.ConnectException e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.UNHEALTHY)
                    .message("Could not connect to GitHub API: " + e.getMessage())
                    .error(e)
                    .build();
        } catch (java.net.SocketTimeoutException e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.DEGRADED)
                    .message("Timeout while connecting to or communicating with GitHub API: " + e.getMessage())
                    .error(e)
                    .build();
        } catch (Exception e) {
            return HealthCheckResult.builder()
                    .status(HealthStatus.FAILED)
                    .message("Unexpected error during GitHub API health check: " + e.getMessage())
                    .error(e)
                    .build();
        }
    }
}
