package ts.andrey.kafkaproducer.model;

import io.opentelemetry.api.trace.Span;
import lombok.Getter;

@Getter
public class GenerateResponse {

    public GenerateResponse(long deviceCount, long messageCount) {
        this.messageCount = messageCount;
        this.deviceCount = deviceCount;
        trace = Span.current().getSpanContext().getTraceId();
    }

    private final long messageCount;
    private final long deviceCount;
    private final String trace;

}
