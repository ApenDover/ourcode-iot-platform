package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

import java.util.List;

public interface DeviceEventService {

    void saveEvents(List<DeviceEvent> events);

}
