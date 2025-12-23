package ts.andrey.kafkaproducer.config;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.opentelemetry.api.trace.Span;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;
import ts.andrey.kafkaproducer.utils.LogMaskUtil;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

@Slf4j
@Component
public class LoggingReqResFilter extends OncePerRequestFilter {

    private static final List<String> NOT_LOGGED_ENDPOINTS = List.of(
            "/actuator"
    );

    private static final String TRACE = "traceId";
    private static final String SPAN = "spanId";
    private static final int MAX_BODY_SIZE_BYTES = 1024 * 1024;
    private static final int MAX_TEXT_PREVIEW_CHARS = 1000;
    private static final String EMPTY_BODY = "[Empty]";
    private static final String BODY_TOO_LARGE_BYTES = "[Body exceeds max size: " + MAX_BODY_SIZE_BYTES + " bytes]";
    private static final String MASKED_TEXT = "***";

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.log.beautify:false}")
    private boolean isBeautify;

    @Value("${app.log.masking:false}")
    private boolean isMasking;

    @Value("${app.log.keys:null}")
    private Set<String> keyForMasking;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        MDC.put(TRACE, Span.current().getSpanContext().getTraceId());
        MDC.put(SPAN, Span.current().getSpanContext().getSpanId());

        if (shouldSkipLogging(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        final var wrappedRequest = new ContentCachingRequestWrapper(request);
        final var wrappedResponse = new ContentCachingResponseWrapper(response);

        try {
            filterChain.doFilter(wrappedRequest, wrappedResponse);
        } finally {
            try {
                logRequestAndResponse(wrappedRequest, wrappedResponse);
            } catch (Exception e) {
                log.warn("Failed to log request/response for URI: {}",
                        request.getRequestURI(), e);
            } finally {
                MDC.clear();
                wrappedResponse.copyBodyToResponse();
            }
        }
    }

    /**
     * Проверяет, нужно ли пропускать логирование для данного запроса
     */
    private boolean shouldSkipLogging(HttpServletRequest request) {
        final var requestUri = request.getRequestURI();
        return NOT_LOGGED_ENDPOINTS.stream()
                .anyMatch(requestUri::startsWith);
    }

    /**
     * Логирует запрос и ответ
     */
    private void logRequestAndResponse(ContentCachingRequestWrapper request,
                                       ContentCachingResponseWrapper response) throws IOException {
        final byte[] requestBodyBytes = request.getContentAsByteArray();
        final byte[] responseBodyBytes = response.getContentAsByteArray();

        final boolean isRequestBodyTooLarge = isBodyTooLargeByBytes(requestBodyBytes);
        final boolean isResponseBodyTooLarge = isBodyTooLargeByBytes(responseBodyBytes);

        final String requestBody = isRequestBodyTooLarge
                ? BODY_TOO_LARGE_BYTES : extractBody(requestBodyBytes, request.getCharacterEncoding());

        final String responseBody = isResponseBodyTooLarge
                ? BODY_TOO_LARGE_BYTES
                : extractBody(responseBodyBytes, response.getCharacterEncoding());

        final var loggedRequestBody = processBodyForLogging(requestBody, isRequestBodyTooLarge);
        final var loggedResponseBody = processBodyForLogging(responseBody, isResponseBodyTooLarge);

        final var requestHeaders = formatHeaders(request);
        final var responseHeaders = formatHeaders(response);

        log.info("Request: {} {} | Headers: {} | Body: {}",
                request.getMethod(),
                request.getRequestURI(),
                requestHeaders,
                loggedRequestBody);

        log.info("Response: {} | Status: {} | Headers: {} | Body: {}",
                request.getRequestURI(),
                response.getStatus(),
                responseHeaders,
                loggedResponseBody);
    }

    /**
     * Извлекает тело из массива байт
     */
    private String extractBody(byte[] content, String encoding) {
        if (content == null || content.length == 0) {
            return "";
        }
        try {
            Charset charset = (encoding != null && Charset.isSupported(encoding))
                    ? Charset.forName(encoding)
                    : StandardCharsets.UTF_8;
            return new String(content, charset);
        } catch (Exception e) {
            log.warn("Failed to extract body with encoding: {}", encoding, e);
            return new String(content, StandardCharsets.UTF_8);
        }
    }

    /**
     * Проверяет, не превышает ли тело максимальный размер в байтах
     */
    private boolean isBodyTooLargeByBytes(byte[] body) {
        return body != null && body.length > MAX_BODY_SIZE_BYTES;
    }

    /**
     * Обрабатывает тело для логирования: маскирует, форматирует, обрезает
     */
    private String processBodyForLogging(String body, boolean isBodyTooLargeByBytes) {
        if (body == null || body.isEmpty()) {
            return EMPTY_BODY;
        }

        if (isBodyTooLargeByBytes) {
            return BODY_TOO_LARGE_BYTES;
        }

        if (body.length() > MAX_TEXT_PREVIEW_CHARS) {
            return truncateText(body);
        }

        try {
            final var originalNode = objectMapper.readTree(body);
            final var nodeForLogging = applyMasking(originalNode);
            return formatJson(nodeForLogging, isBeautify);
        } catch (JsonProcessingException e) {
            return processTextBody(body);
        }
    }

    /**
     * Применяет маскирование к JSON узлу
     */
    private JsonNode applyMasking(JsonNode node) {
        if (!isMasking || keyForMasking == null || keyForMasking.isEmpty()) {
            return node;
        }
        return LogMaskUtil.mask(node, keyForMasking);
    }

    /**
     * Форматирует JSON для вывода в лог
     */
    private String formatJson(JsonNode node, boolean beautify) throws JsonProcessingException {
        String jsonString = beautify
                ? objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node)
                : objectMapper.writeValueAsString(node);

        if (jsonString.length() > MAX_TEXT_PREVIEW_CHARS) {
            return truncateText(jsonString);
        }
        return jsonString;
    }

    /**
     * Обрабатывает не-JSON тело (текст)
     */
    private String processTextBody(String text) {
        if (isMasking) {
            return MASKED_TEXT;
        }
        return text;
    }

    /**
     * Обрезает текст если он слишком длинный
     */
    private String truncateText(String text) {
        if (text.length() <= MAX_TEXT_PREVIEW_CHARS) {
            return text;
        }
        return text.substring(0, MAX_TEXT_PREVIEW_CHARS)
                + String.format("... [truncated, total: %d chars]", text.length());
    }

    /**
     * Форматирует заголовки запроса
     */
    private String formatHeaders(HttpServletRequest request) {
        var headers = new StringBuilder("{");
        var names = request.getHeaderNames();
        boolean first = true;
        while (names.hasMoreElements()) {
            if (!first) {
                headers.append(", ");
            }
            var name = names.nextElement();
            String value = request.getHeader(name);
            if ("authorization".equalsIgnoreCase(name)) {
                value = "JWT_TOKEN";
            }
            headers.append(name).append("=").append(value);
            first = false;
        }
        headers.append("}");
        return headers.toString();
    }

    /**
     * Форматирует заголовки ответа
     */
    private String formatHeaders(HttpServletResponse response) {
        var headers = new StringBuilder("{");
        var headerNames = response.getHeaderNames();
        boolean first = true;
        for (String name : headerNames) {
            if (!first) {
                headers.append(", ");
            }
            String value = response.getHeader(name);
            if ("authorization".equalsIgnoreCase(name)) {
                value = "JWT_TOKEN";
            }
            headers.append(name).append("=").append(value);
            first = false;
        }
        headers.append("}");
        return headers.toString();
    }

}
