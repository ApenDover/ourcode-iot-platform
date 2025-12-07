package ts.andrey.eventservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EventServiceMetrics {

    private final MeterRegistry meterRegistry;

    public void incrementSuccess(String uri, String method, int status) {
        meterRegistry.counter("event_service_success",
                        "method", method,
                        "uri", normalizeUri(uri),
                        "status", String.valueOf(status),
                        "statusGroup", statusGroup(status))
                .increment();
    }

    public void incrementError(String uri, String method, int status, Throwable ex) {
        meterRegistry.counter("event_service_fail",
                        "method", method,
                        "uri", normalizeUri(uri),
                        "status", String.valueOf(status),
                        "statusGroup", statusGroup(status),
                        "exception", ex != null ? ex.getClass().getSimpleName() : "unknown")
                .increment();

    }

    public void recordRequestTime(String method, String uri, long durationNs) {
        Timer.builder("event_service_request_duration_seconds")
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(100), Duration.ofMillis(500), Duration.ofSeconds(1))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

    private String statusGroup(int status) {
        if (status >= 200 && status < 300) return "2xx";
        if (status >= 400 && status < 500) return "4xx";
        if (status >= 500) return "5xx";
        return "other";
    }

    private String normalizeUri(String uri) {
        String normalized = uri.split("\\?")[0]; // Убираем query params
        normalized = normalized.replaceAll("/\\d+$", "/{id}");
        normalized = normalized.replaceAll("/[0-9a-fA-F-]{36}$", "/{uuid}");
        return normalized;
    }

}
