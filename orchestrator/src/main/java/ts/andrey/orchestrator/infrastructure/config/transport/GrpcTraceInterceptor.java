package ts.andrey.orchestrator.infrastructure.config.transport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.Message;
import com.google.protobuf.util.JsonFormat;
import io.grpc.*;
import io.opentelemetry.api.trace.Span;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class GrpcTraceInterceptor implements ClientInterceptor {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public <ReqT, RespT> ClientCall<ReqT, RespT> interceptCall(
            MethodDescriptor<ReqT, RespT> method,
            CallOptions callOptions,
            Channel next) {

        return new ForwardingClientCall.SimpleForwardingClientCall<ReqT, RespT>(
                next.newCall(method, callOptions)) {

            private final String methodName = method.getFullMethodName();
            private Instant startTime;
            private Metadata requestHeaders;

            @Override
            public void start(Listener<RespT> responseListener, Metadata headers) {
                startTime = Instant.now();
                requestHeaders = headers;

                // Добавляем trace context
                final var currentSpan = Span.current();
                if (currentSpan.getSpanContext().isValid()) {
                    final var traceparent = String.format("00-%s-%s-%s",
                            currentSpan.getSpanContext().getTraceId(),
                            currentSpan.getSpanContext().getSpanId(),
                            currentSpan.getSpanContext().getTraceFlags().asHex());

                    headers.put(Metadata.Key.of("traceparent",
                            Metadata.ASCII_STRING_MARSHALLER), traceparent);

                    final var traceState = currentSpan.getSpanContext().getTraceState();
                    if (traceState != null && traceState.size() > 0) {
                        headers.put(Metadata.Key.of("tracestate",
                                Metadata.ASCII_STRING_MARSHALLER), traceState.toString());
                    }
                }

                super.start(new ForwardingClientCallListener<RespT>() {
                    @Override
                    protected Listener<RespT> delegate() {
                        return responseListener;
                    }

                    @Override
                    public void onMessage(RespT message) {
                        long duration = java.time.Duration.between(startTime, Instant.now()).toMillis();
                        Map<String, Object> logData = new HashMap<>();
                        logData.put("method", methodName);
                        logData.put("type", "response");
                        logData.put("duration_ms", duration);
                        logData.put("response", messageToString(message));

                        try {
                            log.info("gRPC Response: {}", objectMapper.writeValueAsString(logData));
                        } catch (Exception e) {
                            log.info("gRPC Response: method={}, duration={}ms", methodName, duration);
                        }
                        super.onMessage(message);
                    }

                    @Override
                    public void onClose(Status status, Metadata trailers) {
                        super.onClose(status, trailers);
                    }
                }, headers);
            }

            @Override
            public void sendMessage(ReqT message) {
                // Структурированный лог с заголовками и телом
                Map<String, Object> logData = new HashMap<>();
                logData.put("method", methodName);
                logData.put("type", "request");
                logData.put("headers", metadataToMap(requestHeaders));
                logData.put("body", messageToString(message));

                try {
                    log.info("gRPC Request: {}", objectMapper.writeValueAsString(logData));
                } catch (Exception e) {
                    // Fallback на обычный лог
                    log.info("gRPC Request: method={}, headers={}, body={}",
                            methodName, metadataToString(requestHeaders), messageToString(message));
                }
                super.sendMessage(message);
            }
        };
    }

    private Map<String, String> metadataToMap(Metadata metadata) {
        Map<String, String> map = new HashMap<>();
        if (metadata == null) return map;

        for (String key : metadata.keys()) {
            if (!key.endsWith("-bin")) {
                Metadata.Key<String> stringKey = Metadata.Key.of(key, Metadata.ASCII_STRING_MARSHALLER);
                String value = metadata.get(stringKey);
                if (value != null) {
                    map.put(key, value);
                }
            }
        }
        return map;
    }

    private String metadataToString(Metadata metadata) {
        if (metadata == null) return "{}";
        return metadataToMap(metadata).toString();
    }

    private <T> String messageToString(T message) {
        if (message == null) return null;

        try {
            if (message instanceof Message) {
                return JsonFormat.printer()
                        .includingDefaultValueFields()
                        .print((Message) message);
            }
            return message.toString();
        } catch (Exception e) {
            return message.getClass().getSimpleName();
        }
    }

}
