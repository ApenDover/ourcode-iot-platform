package ts.andrey.eventcollector.utils;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.experimental.UtilityClass;
import org.apache.kafka.common.header.Headers;
import org.slf4j.MDC;

import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;

@UtilityClass
public class TraceUtil {

    private static final String TRACE = "traceparent";
    private static final String MDC_TRACE = "trace-id";
    private static final String MDC_SPAN = "span-id";

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

    public String getTraceParentFromIterator(Headers headers) {
        while (headers.iterator().hasNext()) {
            final var header = headers.iterator().next();
            if (header.key().equals(TRACE)) {
                return new String(header.value(), StandardCharsets.UTF_8);
            }
        }
        return "";
    }

    public <T> T withSpan(Tracer tracer, String spanName, Supplier<T> code) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return code.get();
        } finally {
            span.end();
        }
    }

    public void withSpan(Tracer tracer, String spanName, Runnable code) {
        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            code.run();
        } finally {
            span.end();
        }
    }

    public <T> T withRootSpan(String trace, Supplier<T> code) {
        SpanContext parentContext = spanContextFromTraceParent(trace);
        MDC.put(MDC_TRACE, parentContext.getTraceId());
        MDC.put(MDC_SPAN, parentContext.getSpanId());
        Span parentSpan = Span.wrap(parentContext);
        try (Scope scope = parentSpan.makeCurrent()) {
            return code.get();
        }
    }

    public void withRootSpan(String trace, Runnable code) {
        SpanContext parentContext = spanContextFromTraceParent(trace);
        MDC.put(MDC_TRACE, parentContext.getTraceId());
        MDC.put(MDC_SPAN, parentContext.getSpanId());
        Span parentSpan = Span.wrap(parentContext);
        try (Scope scope = parentSpan.makeCurrent()) {
            code.run();
        }
    }

}
