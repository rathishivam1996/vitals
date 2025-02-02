package org.vitals.core;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import com.google.common.base.Preconditions;

public interface HealthCheck {

    HealthCheckResult check() throws Exception;

    String getName();

    default Set<String> getTags() {
        return Collections.emptySet();
    }

    class HealthCheckResult {
        private final HealthStatus status;
        private final String message;
        private final Throwable error;
        private final Duration timeToLive;

        // Custom key-value data
        private final Map<String, byte[]> data;

        // Private constructor to enforce usage of the builder
        protected HealthCheckResult(Builder<?> builder) {
            this.status = builder.status;
            this.message = builder.message;
            this.error = builder.error;
            this.data = builder.data != null ? Map.copyOf(builder.data) : Collections.emptyMap();
            this.timeToLive = builder.timeToLive != null ? builder.timeToLive : Duration.ZERO;
        }

        public HealthStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Throwable getError() {
            return error;
        }

        public Duration getTimeToLive() {
            return timeToLive;
        }

        public Map<String, byte[]> getData() {
            return data;
        }

        public static Builder<?> builder() {
            return new Builder<>();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;
            HealthCheckResult that = (HealthCheckResult) o;
            return status == that.status && Objects.equals(message, that.message) && Objects.equals(error,
                    that.error) && Objects.equals(timeToLive, that.timeToLive) && data.equals(that.data);
        }

        @Override
        public int hashCode() {
            return Objects.hash(status, message, error, timeToLive, data);
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("HealthCheckResult{");
            sb.append(", status=").append(status);
            sb.append(", message='").append(message).append('\'');
            sb.append(", error=").append(error);
            sb.append(", timeToLive=").append(timeToLive);
            sb.append(", data=").append(data);
            sb.append('}');
            return sb.toString();
        }

        public static class Builder<T extends Builder<T>> {
            private HealthStatus status = HealthStatus.INITIALIZING;
            private String message;
            private Throwable error;
            private Duration timeToLive;
            private Map<String, byte[]> data = new HashMap<>();

            public T status(HealthStatus status) {
                this.status = Preconditions.checkNotNull(status, "HealthStatus must not be null");
                return self();
            }

            public T message(String message) {
                this.message = message;
                return self();
            }

            public T error(Throwable error) {
                this.error = error;
                return self();
            }

            public T timeToLive(Duration ttl) {
                this.timeToLive = Preconditions.checkNotNull(ttl, "TTL must not be null");
                return self();
            }

            public T addData(String key, byte[] value) {
                Preconditions.checkNotNull(key, "Data key must not be null");
                Preconditions.checkNotNull(value, "Data value must not be null");
                this.data.put(key, value);
                return self();
            }

            public T from(HealthCheckResult result) {
                Preconditions.checkNotNull(result, "HealthCheckResult must not be null");
                this.status = result.status;
                this.message = result.message;
                this.error = result.error;
                this.timeToLive = result.timeToLive;
                this.data = new HashMap<>(result.data);
                return self();
            }

            @SuppressWarnings("unchecked")
            protected T self() {
                return (T) this;
            }

            // Build method to create an instance of HealthCheckResult
            public HealthCheckResult build() {
                return new HealthCheckResult(this);
            }
        }
    }

    enum HealthStatus {

        HEALTHY("Fully operational, no issues."),
        UNHEALTHY("Significant issues, action needed."),
        FAILED("Internal error occurred during health check execution."),
        DEGRADED("Operational but with reduced functionality or performance."),
        UNKNOWN("State is undetermined, possibly due to lack of data."),
        CRITICAL("Severe issues requiring immediate attention."),
        INITIALIZING("Component is starting up, health unknown."),
        MAINTENANCE("Component intentionally offline for maintenance."),
        WARNING("Potential issue detected; requires attention."),
        RECOVERING("Transitioning from unhealthy to healthy."),
        DISABLED("Checks are intentionally disabled or paused.");

        private final String description;

        HealthStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }

        @Override
        public String toString() {
            return name() + " - " + description;
        }
    }

}