package ts.andrey.failedeventsprocessor.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class FailEventMetrics {

    private final MeterRegistry meterRegistry;

    public void recordProcessed(ErrorType errorType) {
        meterRegistry.counter(
                "fails.handler.processed",
                "errorType", errorType.name()
        ).increment();
    }

}
