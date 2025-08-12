package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;

public interface CollectorService {

    void collect(DeviceEvent event);

}
