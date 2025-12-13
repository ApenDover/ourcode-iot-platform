package ts.andrey.orchestrator.infrastructure.config.transport;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Client;
import feign.Request;
import feign.Response;
import lombok.extern.slf4j.Slf4j;
import ts.andrey.orchestrator.infrastructure.util.LogMaskUtil;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
public class UniversalLoggingFeignClient implements Client {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int MAX_LOG_BODY_SIZE = 1024 * 1024;
    private static final int MAX_TEXT_PREVIEW = 500;

    private final Client delegate;
    private final boolean beautify;
    private final boolean maskData;
    private final Set<String> maskKeys;
    private final boolean logEnabled;

    public UniversalLoggingFeignClient(Client delegate, boolean beautify,
                                       boolean maskData, Set<String> maskKeys) {
        this.delegate = delegate;
        this.beautify = beautify;
        this.maskData = maskData;
        this.maskKeys = maskKeys != null ? maskKeys : Set.of();
        this.logEnabled = log.isInfoEnabled();
    }

    @Override
    public Response execute(Request request, Request.Options options) throws IOException {
        if (logEnabled) {
            logRequest(request);
        }

        Response response = delegate.execute(request, options);

        if (logEnabled) {
            response = logAndReplaceResponse(request, response);
        }

        return response;
    }

    private void logRequest(Request request) throws IOException {
        byte[] bodyBytes = request.body();

        if (request.httpMethod().equals(Request.HttpMethod.POST) && bodyBytes != null) {
            logBody("Request", request.url(), maskToken(request.headers()), bodyBytes, null);
        } else {
            log.info("GET request to: {} headers: {}", request.url(), request.headers());
        }
    }

    private Response logAndReplaceResponse(Request request, Response response) throws IOException {
        if (response.body() == null) {
            log.info("Response from: {} status: {} headers: {} body: [Empty]",
                    request.url(), response.status(), request.headers());
            return response;
        }

        byte[] responseBodyBytes;
        try (InputStream bodyStream = response.body().asInputStream()) {
            responseBodyBytes = bodyStream.readAllBytes();
        }

        logBody("Response", request.url(), request.headers(), responseBodyBytes, response.status());

        return Response.builder()
                .body(responseBodyBytes)
                .headers(response.headers())
                .reason(response.reason())
                .status(response.status())
                .request(response.request())
                .build();
    }

    private void logBody(String type, String url, Map<String, Collection<String>> headers,
                         byte[] bodyBytes, Integer status) throws IOException {
        if (bodyBytes == null || bodyBytes.length == 0) {
            logWithStatus(type, url, maskToken(headers), status, "[Empty body]");
            return;
        }

        if (bodyBytes.length > MAX_LOG_BODY_SIZE) {
            logWithStatus(type, url, maskToken(headers), status,
                    String.format("[Body too large to log: %d bytes]", bodyBytes.length));
            return;
        }

        try {
            final var node = OBJECT_MAPPER.readTree(bodyBytes);
            if (maskData && !maskKeys.isEmpty()) {
                LogMaskUtil.mask(node, maskKeys);
            }

            String body = beautify
                    ? OBJECT_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node)
                    : OBJECT_MAPPER.writeValueAsString(node);
            String truncated = body.length() > MAX_TEXT_PREVIEW
                    ? body.substring(0, MAX_TEXT_PREVIEW) + "... [truncated]"
                    : body;
            logWithStatus(type, url, headers, status, truncated);
        } catch (JsonParseException e) {
            String bodyText = new String(bodyBytes, StandardCharsets.UTF_8);
            String truncated = bodyText.length() > MAX_TEXT_PREVIEW
                    ? bodyText.substring(0, MAX_TEXT_PREVIEW) + "... [truncated]"
                    : bodyText;

            if (maskData) {
                logWithStatus(type, url, headers, status, "***");
            } else {
                logWithStatus(type, url, headers, status, truncated);
            }
        }
    }

    private void logWithStatus(String type, String url, Map<String, Collection<String>> headers,
                               Integer status, String body) {
        if (status != null) {
            log.info("{} from: {} status: {} headers: {} body: {}",
                    type, url, status, maskToken(headers), body);
        } else {
            log.info("{} to: {} headers: {} body: {}",
                    type, url, maskToken(headers), body);
        }
    }

    public Map<String, Collection<String>> maskToken(Map<String, Collection<String>> headers) {
        Map<String, Collection<String>> result = new HashMap<>(headers);
        if (result.containsKey("Authorization")) {
            result.put("Authorization", List.of("token"));
        }
        return result;
    }

}
