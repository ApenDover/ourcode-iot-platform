package ts.andrey.eventservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.context.request.NativeWebRequest;
import ts.andrey.api.EventsV1Api;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;

import java.util.Optional;

@RestController
@RequiredArgsConstructor
public class EventController implements EventsV1Api {

    @Override
    public Optional<NativeWebRequest> getRequest() {
        return EventsV1Api.super.getRequest();
    }

    @Override
    public ResponseEntity<Event> apiV1EventsEventIdGet(String eventId, String deviceId) {
        return EventsV1Api.super.apiV1EventsEventIdGet(eventId, deviceId);
    }

    @Override
    public ResponseEntity<EventPage> apiV1EventsGet(String deviceId, Long fromTimestamp, Long toTimestamp, String type, Integer page, Integer size) {
        return EventsV1Api.super.apiV1EventsGet(deviceId, fromTimestamp, toTimestamp, type, page, size);
    }

}
