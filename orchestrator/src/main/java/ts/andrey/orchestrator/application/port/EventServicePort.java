package ts.andrey.orchestrator.application.port;

import ts.andrey.orchestrator.dto.Event;
import ts.andrey.orchestrator.dto.EventPage;

public interface EventServicePort {

    Event getEvent(String eventId, String deviceId);

    EventPage getEventByFilter(String deviceId, Long fromMs, Long toMs, String eventType, Integer page, Integer size);

}
