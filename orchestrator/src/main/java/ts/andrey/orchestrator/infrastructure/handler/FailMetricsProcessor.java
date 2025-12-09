package ts.andrey.orchestrator.infrastructure.handler;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.domain.ApiEndpoint;
import ts.andrey.orchestrator.domain.metrics.OrchestratorMetrics;

import java.net.URI;

@Slf4j
@Component
@RequiredArgsConstructor
public class FailMetricsProcessor {

    private final OrchestratorMetrics metrics;

    public void sendFail(String method, String requestUri, Throwable ex) {
        try {
            ApiEndpoint endpoint = resolveEndpoint(method, requestUri);

            if (endpoint != null) {
                sendFailMetric(endpoint);
                log.debug("Отправлена fail метрика для endpoint: {}", endpoint);
            } else {
                log.warn("Не удалось определить endpoint для метода {} и URI: {}", method, requestUri);
            }

            int status = determineStatusCode(ex);
            metrics.recordFailure(method, normalizeUriForMetrics(requestUri), status, ex);

        } catch (Exception e) {
            log.error("Ошибка при обработке fail метрики для URI: {}", requestUri, e);
        }
    }

    public void sendFail(String method, String requestUri) {
        sendFail(method, requestUri, null);
    }

    ApiEndpoint resolveEndpoint(String method, String requestUri) {
        String normalizedUri = normalizeUri(requestUri);
        for (ApiEndpoint endpoint : ApiEndpoint.values()) {
            if (endpoint.equals(ApiEndpoint.GET_DEVICES)) {
                System.out.println();
            }
            if (endpoint.getMethod().equalsIgnoreCase(method) &&
                    matchesPath(endpoint.getPath(), normalizedUri)) {
                return endpoint;
            }
        }

        return null;
    }

    private boolean matchesPath(String endpointPath, String requestUri) {
        try {
            final var uri = new URI(requestUri).getPath();
            if (endpointPath.equals(uri)) {
                return true;
            }

            String[] endpointParts = endpointPath.split("/");
            String[] requestParts = uri.split("/");

            if (endpointParts.length != requestParts.length) {
                return false;
            }

            for (int i = 0; i < endpointParts.length; i++) {
                String endpointPart = endpointParts[i];
                String requestPart = requestParts[i];

                if (endpointPart.startsWith("{") && endpointPart.endsWith("}")) {
                    // Проверяем, что параметр не пустой
                    if (requestPart.isEmpty()) {
                        return false;
                    }
                    continue;
                }

                if (!endpointPart.equals(requestPart)) {
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return false;
        }
    }

    private void sendFailMetric(ApiEndpoint endpoint) {
        switch (endpoint) {
            case GET_DEVICES:
                metrics.deviceServiceGetListFail();
                break;
            case CREATE_DEVICE:
                break;
            case GET_DEVICE_BY_ID:
                metrics.deviceServiceGetFail();
                break;
            case UPDATE_DEVICE:
                metrics.deviceUpdateFail();
                break;
            case DELETE_DEVICE:
                metrics.deviceServiceDeleteFail();
                break;

            case GET_EVENTS:
                metrics.eventServiceGetFilterFail();
                break;
            case GET_EVENT_BY_ID:
                metrics.eventServiceGetFail();
                break;

            case SEND_COMMAND:
                metrics.routerCommandFail();
                break;
            case POLL_COMMANDS:
                metrics.routerPollFail();
                break;
            case ACK_COMMAND:
                metrics.routerAckFail();
                break;

            case UPDATE_DEVICE_VERSION:
                metrics.sagaUpdateVersionFail();
                break;
        }
    }

    private String normalizeUri(String uri) {
        int queryIndex = uri.indexOf('?');
        if (queryIndex != -1) {
            uri = uri.substring(0, queryIndex);
        }
        return uri;
    }

    private String normalizeUriForMetrics(String uri) {
        return uri.replaceAll("/[0-9a-fA-F-]{20,}", "/{id}")
                .replaceAll("/[0-9]+", "/{id}")
                .replaceAll("deviceId", "{deviceId}")
                .replaceAll("event_id", "{eventId}");
    }

    private int determineStatusCode(Throwable ex) {
        if (ex instanceof FeignException) {
            return ((FeignException) ex).status();
        }
        return 400;
    }

}
