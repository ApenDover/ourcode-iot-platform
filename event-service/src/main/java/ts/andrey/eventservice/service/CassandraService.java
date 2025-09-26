package ts.andrey.eventservice.service;

import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.model.EventFilterRequest;

public interface CassandraService {

    Event getEvent(String eventId, String deviceId);

    EventPage getEventByFilter(EventFilterRequest eventFilterRequest);

}
