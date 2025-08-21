package ts.andrey.eventcollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CassandraMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;

    public CassandraMetrics(MeterRegistry registry) {
        this.successCounter = Counter.builder("events.cassandra.success")
                .description("Число успешных сохранений в Cassandra")
                .register(registry);

        this.errorCounter = Counter.builder("events.cassandra.error")
                .description("Число ошибок при сохранении в Cassandra")
                .register(registry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementSuccess(int count) {
        successCounter.increment(count);
    }

    public void incrementError() {
        errorCounter.increment();
    }

}
