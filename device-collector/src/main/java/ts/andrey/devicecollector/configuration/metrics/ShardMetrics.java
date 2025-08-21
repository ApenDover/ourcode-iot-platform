package ts.andrey.devicecollector.configuration.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
@RequiredArgsConstructor
public class ShardMetrics {

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
        return Counter.builder("device.processed.success")
                .description("Number of successfully processed devices per shard")
                .tag("shard", shardId)
                .register(registry);
    }

    private Counter createErrorCounter(String shardId) {
        return Counter.builder("device.processed.error")
                .description("Number of errors per shard")
                .tag("shard", shardId)
                .register(registry);
    }

}
