package ts.andrey.eventcollector.annotation;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
public class TracingAspect {

    private final Tracer tracer; // из OpenTelemetry

    @Around("@annotation(withSpan)")
    public Object traceMethod(ProceedingJoinPoint pjp, WithSpan withSpan) throws Throwable {
        String spanName = !withSpan.value().isEmpty()
                ? withSpan.value()
                : pjp.getSignature().getName();

        Span span = tracer.spanBuilder(spanName).startSpan();
        try (Scope scope = span.makeCurrent()) {
            return pjp.proceed();
        } finally {
            span.end();
        }
    }

}
