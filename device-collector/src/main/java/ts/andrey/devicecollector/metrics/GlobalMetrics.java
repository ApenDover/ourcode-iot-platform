package ts.andrey.devicecollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GlobalMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;
    private final Counter dltCounter;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("device.processed.success")
                .description("Общее число успешно обработанных батчей")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("device.processed.error")
                .description("Общее число проблемных батчей")
                .register(meterRegistry);

        this.dltCounter = Counter.builder("device.dlt.error")
                .description("Общее число отправленных в dlt девайсов")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

    public void incrementDltMessage() {
        dltCounter.increment();
    }

}
