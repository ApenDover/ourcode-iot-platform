package ts.andrey.deviceservice.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ts.andrey.deviceservice.metrics.DeviceMetrics;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

@Slf4j
@Component
@RequiredArgsConstructor
public class PerRequestFilter extends OncePerRequestFilter {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final DeviceMetrics deviceMetrics;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        final var start = System.nanoTime();
        Exception exception = null;

        final var wrappedRequest = new ContentCachingRequestWrapper(request);
        final var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } catch (Exception ex) {
            exception = ex;
            throw ex;
        } finally {
            final var durationNs = System.nanoTime() - start;

            final var httpStatus = HttpStatus.resolve(wrappedResponse.getStatus());
            final var isError = httpStatus != null && httpStatus.isError();

            deviceMetrics.recordExecutionTime(
                    wrappedRequest.getMethod(),
                    wrappedRequest.getRequestURI(),
                    durationNs
            );

            if (!isError && exception == null) {
                deviceMetrics.recordSuccess(
                        wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(),
                        wrappedResponse.getStatus()
                );
            } else {
                deviceMetrics.recordFailure(
                        wrappedRequest.getMethod(),
                        wrappedRequest.getRequestURI(),
                        wrappedResponse.getStatus(),
                        exception
                );
            }
            logRequest(wrappedRequest);
            logResponse(wrappedResponse, wrappedRequest);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        if (request.getRequestURI().contains("actuator")) {
            return;
        }
        final var body = toSingleLineJson(new String(request.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info("Request: method={}, uri={}, headers={}, body={}",
                request.getMethod(),
                request.getRequestURI(),
                formatHeaders(request),
                body);
    }

    private void logResponse(ContentCachingResponseWrapper response, ContentCachingRequestWrapper request) {
        if (request.getRequestURI().contains("actuator")) {
            return;
        }
        final var body = toSingleLineJson(new String(response.getContentAsByteArray(), StandardCharsets.UTF_8));
        log.info("Response from {}: status={}, headers={}, body={}",
                request.getRequestURI(),
                response.getStatus(),
                formatHeaders(response),
                body);
    }

    private String toSingleLineJson(String body) {
        try {
            final var json = OBJECT_MAPPER.readValue(body, Object.class);
            return OBJECT_MAPPER.writeValueAsString(json);
        } catch (Exception e) {
            return body.replaceAll("[\\r\\n]+", " ");
        }
    }

    private String formatHeaders(HttpServletRequest request) {
        var headers = new StringBuilder("{");
        var names = request.getHeaderNames();
        while (names.hasMoreElements()) {
            var name = names.nextElement();
            String value = request.getHeader(name);
            if ("authorization".equalsIgnoreCase(name)) {
                value = "JWT_TOKEN";
            }
            headers.append(name).append("=")
                    .append(value).append(", ");
        }
        if (headers.length() > 1) {
            headers.setLength(headers.length() - 2);
        }
        headers.append("}");
        return headers.toString();
    }

    private String formatHeaders(HttpServletResponse response) {
        var headers = new StringBuilder("{");
        for (String name : response.getHeaderNames()) {
            String value = response.getHeader(name);
            if ("authorization".equalsIgnoreCase(name)) {
                value = "JWT_TOKEN";
            }
            headers.append(name).append("=")
                    .append(value).append(", ");
        }
        if (headers.length() > 1) {
            headers.setLength(headers.length() - 2);
        }
        headers.append("}");
        return headers.toString();
    }

}
