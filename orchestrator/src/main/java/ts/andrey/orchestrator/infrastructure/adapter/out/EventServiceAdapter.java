package ts.andrey.orchestrator.infrastructure.adapter.out;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.application.port.EventServicePort;
import ts.andrey.orchestrator.dto.Event;
import ts.andrey.orchestrator.dto.EventPage;
import ts.andrey.orchestrator.infrastructure.mapper.EventMapper;
import ts.andrey.orchestrator.infrastructure.out.port.feign.EventServiceClient;
import ts.andrey.orchestrator.infrastructure.util.RequestTimerUtil;

@Component
@RequiredArgsConstructor
public class EventServiceAdapter implements EventServicePort {

    private final EventServiceClient eventServiceClient;
    private final EventMapper eventMapper;
    private final RequestTimerUtil requestTimerUtil;

    @Override
    @WithSpan("EventTransportGet")
    public Event getEvent(String eventId, String deviceId) {
        return requestTimerUtil.recordExternal("event-service", "getEvent", "http", () -> {
            final var response = eventServiceClient.apiV1EventsEventIdGet(eventId, deviceId);
            return eventMapper.toOrchestratorEventDto(response.getBody());
        });
    }

    @Override
    @WithSpan("EventTransportGetByFilter")
    public EventPage getEventByFilter(
            String deviceId, Long fromMs, Long toMs,
            String eventType, String pageToken
    ) {
        return requestTimerUtil.recordExternal("event-service", "getEvents", "http", () -> {
            final var response = eventServiceClient.apiV1EventsGet(deviceId, fromMs, toMs, eventType, pageToken);
            return eventMapper.toOrchestratorEventPageDto(response.getBody());
        });
    }

}
