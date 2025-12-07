package ts.andrey.devicecollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class PostgresMetrics {

    private final MeterRegistry registry;

    private final Map<String, Counter> successCounters = new ConcurrentHashMap<>();
    private final Map<String, Counter> errorCounters = new ConcurrentHashMap<>();

    public void incrementSuccess(String shardId) {
        successCounters.computeIfAbsent(shardId, this::createSuccessCounter)
                .increment();
    }

    public void incrementError(String shardId) {
        errorCounters.computeIfAbsent(shardId, this::createErrorCounter)
                .increment();
    }

    private Counter createSuccessCounter(String shardId) {
        return Counter.builder("device_collector_postgres_success")
                .description("Число успешных сохранений на шард")
                .tag("shard", shardId)
                .register(registry);
    }

    private Counter createErrorCounter(String shardId) {
        return Counter.builder("device_collector_postgres_fail")
                .description("Число ошибок при сохранении на шард")
                .tag("shard", shardId)
                .register(registry);
    }

}
