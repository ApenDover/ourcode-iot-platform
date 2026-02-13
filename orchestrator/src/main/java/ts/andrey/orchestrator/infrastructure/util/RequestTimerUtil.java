package ts.andrey.orchestrator.infrastructure.util;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.function.Supplier;

@Component
@RequiredArgsConstructor
public class RequestTimerUtil {

    private final MeterRegistry meterRegistry;

    public <T> T recordExternal(String service, String operation, String protocol, Supplier<T> supplier) {
        final var sample = Timer.start(meterRegistry);
        try {
            final var result = supplier.get();
            sample.stop(timer(service, operation, protocol, "success"));
            return result;
        } catch (Exception ex) {
            sample.stop(timer(service, operation, protocol, "error"));
            throw ex;
        }
    }

    private Timer timer(String service, String operation, String protocol, String outcome) {
        return Timer.builder("orchestrator.external.requests")
                .description("External calls from orchestrator")
                .tags("service", service, "operation", operation, "protocol", protocol, "outcome", outcome)
                .publishPercentileHistogram()
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(meterRegistry);
    }

}
