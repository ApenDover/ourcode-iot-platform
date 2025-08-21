package ts.andrey.devicecollector.configuration.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class GlobalMetrics {

    private final Counter successCounter;
    private final Counter errorCounter;

    public GlobalMetrics(MeterRegistry meterRegistry) {
        this.successCounter = Counter.builder("device.processed.success")
                .description("Total number of successfully processed devices")
                .register(meterRegistry);

        this.errorCounter = Counter.builder("device.processed.error")
                .description("Total number of devices that failed processing (sent to DLT)")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        successCounter.increment();
    }

    public void incrementError() {
        errorCounter.increment();
    }

}
