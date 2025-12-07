package ts.andrey.eventcollector.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class EventCollectorMetrics {

    private static final int SLA_ONE = 100;
    private static final int SLA_TWO = 500;
    private static final int SLA_THREE = 1;
    private final MeterRegistry meterRegistry;

    public void recordTime(long durationNs) {
        Timer.builder("event_collector_duration_seconds")
                .description("Время выполнения операции")
                .publishPercentileHistogram(true)
                .sla(Duration.ofMillis(SLA_ONE), Duration.ofMillis(SLA_TWO), Duration.ofSeconds(SLA_THREE))
                .register(meterRegistry)
                .record(Duration.ofNanos(durationNs));
    }

    public void incrementSuccess() {
        meterRegistry.counter("event_collector_success").increment();
    }

    public void incrementError() {
        meterRegistry.counter("event_collector_fail").increment();
    }

    public void incrementDltMessage() {
        meterRegistry.counter("event_collector_dlt").increment();
    }

    public void incrementCassandraSuccess() {
        meterRegistry.counter("event_collector_cassandra_success").increment();
    }

    public void incrementCassandraError() {
        meterRegistry.counter("event_collector_cassandra_fail").increment();
    }

}
