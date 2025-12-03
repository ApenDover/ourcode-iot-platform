package ts.andrey.deviceservice.utils;

import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import lombok.experimental.UtilityClass;

@UtilityClass
public class TraceUtil {

    private static final String TRACE = "traceparent";
    private static final String MDC_TRACE = "trace_id";
    private static final String MDC_SPAN = "span_id";

    /**
     * Преобразует traceparent в SpanContext
     *
     * @param traceparent строка traceparent в формате W3C
     * @return SpanContext
     */
    public static SpanContext spanContextFromTraceParent(String traceparent) {
        if (traceparent == null || traceparent.isEmpty()) {
            return SpanContext.getInvalid();
        }

        final var parts = traceparent.split("-");
        if (parts.length != 4) {
            return SpanContext.getInvalid();
        }

        final var traceId = parts[1];
        final var spanId = parts[2];
        final var flags = (byte) Integer.parseInt(parts[3], 16);

        return SpanContext.create(
                traceId,
                spanId,
                TraceFlags.fromByte(flags),
                TraceState.getDefault()
        );
    }

}
