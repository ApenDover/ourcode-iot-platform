package ts.andrey.devicecollector.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class DeviceCollectorMetrics {

    private static final int SLA_ONE = 100;
    private static final int SLA_TWO = 500;
    private static final int SLA_THREE = 1;

    private final MeterRegistry meterRegistry;

    public void incrementSuccess() {
        meterRegistry.counter("device_collector_success").increment();
    }

    public void incrementError() {
        meterRegistry.counter("device_collector_fail").increment();
    }

    public void incrementDltMessage() {
        meterRegistry.counter("device_collector_dlt").increment();
    }

    public void recordTime(long durationNs) {
        Timer.builder("device_collector_duration_seconds")
                .description("Время выполнения операции")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(SLA_ONE), Duration.ofMillis(SLA_TWO), Duration.ofSeconds(SLA_THREE))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

}
