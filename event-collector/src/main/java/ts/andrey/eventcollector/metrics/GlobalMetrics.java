package ts.andrey.eventcollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GlobalMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter errorDltCounter;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("events.processed.success")
                .description("Общее число успешно обработанных (batch) событий")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("events.processed.error")
                .description("Общее число проблемых (batch) событий")
                .register(meterRegistry);

        this.errorDltCounter = Counter.builder("events.dlt.error")
                .description("Общее число не обработанных событий (отправленно в DLT)")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }


    public void incrementError() {
        errorCounter.increment();
    }

    public void incrementDltError() {
        errorDltCounter.increment();
    }

}
