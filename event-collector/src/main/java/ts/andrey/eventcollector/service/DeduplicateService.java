package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

import java.util.List;

public interface DeduplicateService {

    List<DeviceEvent> getUniqDeviceEvents(List<DeviceEvent> deviceEvents);

}
