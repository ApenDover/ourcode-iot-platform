package ts.andrey.orchestrator.infrastructure.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class OrchestratorMetrics {

    private static final int SLA_ONE = 100;
    private static final int SLA_TWO = 500;
    private static final int SLA_THREE = 1;

    private final MeterRegistry meterRegistry;

    public void getOrchestratorSuccess() {
        meterRegistry.counter("orchestrator.database.get").increment();
    }

    public void deleteOrchestratorSuccess() {
        meterRegistry.counter("orchestrator.database.delete").increment();
    }

    public void updateOrchestratorSuccess() {
        meterRegistry.counter("orchestrator.database.update").increment();
    }

    public void createOrchestratorSuccess() {
        meterRegistry.counter("orchestrator.database.create").increment();
    }

    public void recordSuccess(String method, String uri, int status) {
        meterRegistry.counter("orchestrator.requests.success",
                "method", method,
                "uri", normalizeUri(uri),
                "status", String.valueOf(status),
                "statusGroup", statusGroup(status)
        ).increment();
    }

    public void recordFailure(String method, String uri, int status, Throwable ex) {
        meterRegistry.counter("orchestrator.requests.error",
                "method", method,
                "uri", normalizeUri(uri),
                "status", String.valueOf(status),
                "statusGroup", statusGroup(status),
                "exception", ex != null ? ex.getClass().getSimpleName() : "unknown"
        ).increment();
    }

    public void recordExecutionTime(String method, String uri, long durationNs) {
        Timer.builder("orchestrator.requests.duration")
                .tag("method", method)
                .tag("uri", normalizeUri(uri))
                .description("Время выполнения запроса")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(SLA_ONE), Duration.ofMillis(SLA_TWO), Duration.ofSeconds(SLA_THREE))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

    public void recordDatabaseError(String operation) {
        meterRegistry.counter("orchestrator.database.error",
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
