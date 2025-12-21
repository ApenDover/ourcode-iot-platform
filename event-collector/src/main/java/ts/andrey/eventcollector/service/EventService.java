package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

import java.util.List;

public interface EventService {

    void saveEvents(List<DeviceEvent> events);

}
