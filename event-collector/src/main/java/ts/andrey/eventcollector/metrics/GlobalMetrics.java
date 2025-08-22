package ts.andrey.eventcollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GlobalMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("events.processed.success")
                .description("Число успешно обработанных (batch) событий")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("events.processed.error")
                .description("Общее число не обработанных событий (отправленно в DLT)")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

}
