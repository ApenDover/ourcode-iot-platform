package ts.andrey.orchestrator.infrastructure.config.transport;

import feign.RequestInterceptor;
import feign.RequestTemplate;
import io.opentelemetry.api.trace.Span;
import org.springframework.stereotype.Component;

@Component
public class FeignTraceInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        final var currentSpan = Span.current();
        if (currentSpan.getSpanContext().isValid()) {
            final var traceparent = String.format("00-%s-%s-%s",
                    currentSpan.getSpanContext().getTraceId(),
                    currentSpan.getSpanContext().getSpanId(),
                    currentSpan.getSpanContext().getTraceFlags().asHex());

            template.header("traceparent", traceparent);
        }
    }
}
