package ts.andrey.deviceservice.configuration;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import ts.andrey.deviceservice.metrics.DeviceMetrics;

import java.io.IOException;

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

        try {
            filterChain.doFilter(request, response);
        } catch (Exception ex) {
            exception = ex;
            throw ex;
        } finally {
            final var durationNs = System.nanoTime() - start;

            final var httpStatus = HttpStatus.resolve(response.getStatus());
            final var isError = httpStatus != null && httpStatus.isError();

            deviceMetrics.recordExecutionTime(
                    request.getMethod(),
                    request.getRequestURI(),
                    () -> durationNs
            );

            if (!isError && exception == null) {
                deviceMetrics.recordSuccess(request.getMethod(), request.getRequestURI(), response.getStatus());
            } else {
                deviceMetrics.recordFailure(
                        request.getMethod(), request.getRequestURI(),
                        response.getStatus(), exception
                );
            }
        }
    }

}
