package ts.andrey.devicecollector.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GlobalMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("device.processed.success")
                .description("Общее число успешно обработанных устройств")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("device.processed.error")
                .description("Общее число не обработанных устройств (отправленно в DLT)")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

}
