package ts.andrey.eventservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.api.EventsV1Api;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.model.EventFilterRequest;
import ts.andrey.eventservice.service.CassandraService;

@RestController
@RequiredArgsConstructor
public class EventController implements EventsV1Api {

    private final CassandraService cassandraService;

    @Override
    public ResponseEntity<Event> apiV1EventsEventIdGet(String eventId, String deviceId) {
        return ResponseEntity.ok(cassandraService.getEvent(eventId, deviceId));
    }

    @Override
    public ResponseEntity<EventPage> apiV1EventsGet(
            String deviceId, Long fromTimestamp, Long toTimestamp,
            String type, Integer page, Integer size
    ) {
        final var filter = EventFilterRequest.builder()
                .deviceId(deviceId)
                .fromTimestamp(fromTimestamp)
                .toTimestamp(toTimestamp)
                .type(type)
                .page(page)
                .size(size)
                .build();
        return ResponseEntity.ok(cassandraService.getEventByFilter(filter));
    }

}
