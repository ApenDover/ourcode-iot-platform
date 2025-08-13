package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

import java.util.List;

public interface DeviceService {

    void process(List<DeviceEvent> deviceEvents);

}
