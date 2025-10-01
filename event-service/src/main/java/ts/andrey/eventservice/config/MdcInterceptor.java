package ts.andrey.eventservice.config;

import io.opentelemetry.api.trace.Span;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class MdcInterceptor implements HandlerInterceptor {

    public static final String TRACE = "trace_id";
    private static final String SPAN = "span_id";
    private static final String FLAGS = "trace_flags";
    private static final String SERVICE = "service_name";

    @Value("${spring.application.name}")
    private String appName;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        populateMdcFromSpan();
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response,
                                Object handler, Exception ex) {
        MDC.clear();
    }

    private void populateMdcFromSpan() {
        final var spContext = Span.current().getSpanContext();
        if (spContext != null && spContext.isValid()) {
            MDC.put(TRACE, spContext.getTraceId());
            MDC.put(SPAN, spContext.getSpanId());
            MDC.put(FLAGS, spContext.getTraceFlags().asHex());
        }

        MDC.put(SERVICE, appName);
    }

}
