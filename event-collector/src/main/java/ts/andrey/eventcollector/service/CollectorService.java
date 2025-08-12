package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

import java.util.List;

public interface CollectorService {

    void collect(List<DeviceEvent> event);

}
