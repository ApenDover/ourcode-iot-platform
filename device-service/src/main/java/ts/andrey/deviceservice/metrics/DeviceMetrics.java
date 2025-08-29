package ts.andrey.deviceservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class DeviceMetrics {

    private final MeterRegistry meterRegistry;

    public void getDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.getDevice").increment();
    }

    public void saveDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.saveDevice").increment();
    }

    public void deleteDeviceRedisSuccess() {
        meterRegistry.counter("device.redis.deleteDevice").increment();
    }

    public void deviceRedisSuccess() {
        meterRegistry.counter("device.redis.success").increment();
    }

    public void deviceRedisFailure() {
        meterRegistry.counter("device.redis.error").increment();
    }

    public void getDeviceSuccess() {
        meterRegistry.counter("device.database.getDevice").increment();
    }

    public void deleteDeviceSuccess() {
        meterRegistry.counter("device.database.deleteDevice").increment();
    }

    public void updateDeviceSuccess() {
        meterRegistry.counter("device.database.updateDevice").increment();
    }

    public void createDeviceSuccess() {
        meterRegistry.counter("device.database.createDevice").increment();
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

    public <T> T recordExecutionTime(String method, String uri, Supplier<T> supplier) {
        return Timer.builder("device.requests.duration")
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .description("Время выполнения запроса")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofSeconds(1))
                .register(meterRegistry)
                .record(supplier);
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
