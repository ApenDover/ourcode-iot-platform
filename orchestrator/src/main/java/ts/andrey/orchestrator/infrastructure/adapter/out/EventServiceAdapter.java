package ts.andrey.orchestrator.infrastructure.adapter.out;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import ts.andrey.orchestrator.dto.Event;
import ts.andrey.orchestrator.dto.EventPage;
import ts.andrey.orchestrator.infrastructure.feign.EventServiceClient;
import ts.andrey.orchestrator.infrastructure.mapper.EventMapper;
import ts.andrey.orchestrator.application.outport.EventServicePort;

@Component
@RequiredArgsConstructor
public class EventServiceAdapter implements EventServicePort {

    private final EventServiceClient eventServiceClient;
    private final EventMapper eventMapper;

    @Override
    public Event getEvent(String eventId, String deviceId) {
        final var response = eventServiceClient.apiV1EventsEventIdGet(eventId, deviceId);
        return eventMapper.toOrchestratorEventDto(response.getBody());
    }

    @Override
    public EventPage getEventByFilter(
            String deviceId, Long fromMs, Long toMs,
            String eventType, Integer page, Integer size
    ) {
        final var response = eventServiceClient.apiV1EventsGet(deviceId, fromMs, toMs, eventType, page, size);
        return eventMapper.toOrchestratorEventPageDto(response.getBody());
    }

}
