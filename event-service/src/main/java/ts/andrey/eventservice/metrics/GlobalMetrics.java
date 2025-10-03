package ts.andrey.eventservice.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class GlobalMetrics {

    private final MeterRegistry meterRegistry;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    public void incrementSuccess(String endpoint) {
        Counter.builder("event-service.success.count")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    public void incrementError(String endpoint) {
        Counter.builder("event-service.error.count")
                .tag("endpoint", endpoint)
                .register(meterRegistry)
                .increment();
    }

    public void recordEndpointTime(String method, long durationNs) {
        Timer.builder("event-service.method.timer")
                .tag("method", method)
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

}
