package ts.andrey.deviceservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class DeviceMetrics {

    private static final int SLA_ONE = 100;
    private static final int SLA_TWO = 500;
    private static final int SLA_THREE = 1;

    private final MeterRegistry meterRegistry;

    public void getDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.get").increment();
    }

    public void saveDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.save").increment();
    }

    public void deleteDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.delete").increment();
    }

    public void deviceRedisSuccess() {
        meterRegistry.counter("device.redis.success").increment();
    }

    public void deviceRedisFailure() {
        meterRegistry.counter("device.redis.error").increment();
    }

    public void getDeviceSuccess() {
        meterRegistry.counter("device.database.get").increment();
    }

    public void deleteDeviceSuccess() {
        meterRegistry.counter("device.database.delete").increment();
    }

    public void updateDeviceSuccess() {
        meterRegistry.counter("device.database.update").increment();
    }

    public void createDeviceSuccess() {
        meterRegistry.counter("device.database.create").increment();
    }

    public void recordSuccess(String method, String uri, int status) {
        meterRegistry.counter("device.requests.success",
                "method", method,
                "uri", normalizeUri(uri),
                "status", String.valueOf(status),
                "statusGroup", statusGroup(status)
        ).increment();
    }

    public void recordFailure(String method, String uri, int status, Throwable ex) {
        meterRegistry.counter("device.requests.error",
                "method", method,
                "uri", normalizeUri(uri),
                "status", String.valueOf(status),
                "statusGroup", statusGroup(status),
                "exception", ex != null ? ex.getClass().getSimpleName() : "unknown"
        ).increment();
    }

    public void recordExecutionTime(String method, String uri, long durationNs) {
        Timer.builder("device.requests.duration")
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .description("Время выполнения запроса")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(SLA_ONE), Duration.ofMillis(SLA_TWO), Duration.ofSeconds(SLA_THREE))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

    public void recordDatabaseError(String operation) {
        meterRegistry.counter("device.database.error",
                        "operation", operation)
                .increment();
    }

    private String statusGroup(int status) {
        final var httpStatus = HttpStatusCode.valueOf(status);
        if (httpStatus.is2xxSuccessful()) {
            return "2xx";
        }
        if (httpStatus.is4xxClientError()) {
            return "4xx";
        }
        if (httpStatus.is5xxServerError()) {
            return "5xx";
        }
        return "other";
    }

    private String normalizeUri(String uri) {
        return uri.replaceAll("/[0-9A-Z]{26}$", "/{id}");
    }

}
