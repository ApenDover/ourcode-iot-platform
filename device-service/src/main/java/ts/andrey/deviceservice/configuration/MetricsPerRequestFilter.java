package ts.andrey.deviceservice.configuration;

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
public class MetricsPerRequestFilter extends OncePerRequestFilter {

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
                    () -> durationNs
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
            logResponse(wrappedResponse);
            wrappedResponse.copyBodyToResponse();
        }
    }

    private void logRequest(ContentCachingRequestWrapper request) {
        String body = new String(request.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("Request: method={}, uri={}, headers={}, body={}",
                request.getMethod(),
                request.getRequestURI(),
                request.getHeaderNames(),
                body);
    }

    private void logResponse(ContentCachingResponseWrapper response) {
        String body = new String(response.getContentAsByteArray(), StandardCharsets.UTF_8);
        log.info("Response: status={}, headers={}, body={}",
                response.getStatus(),
                response.getHeaderNames(),
                body);
    }

}
