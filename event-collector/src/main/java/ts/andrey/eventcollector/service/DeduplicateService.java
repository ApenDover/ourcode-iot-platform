package ts.andrey.eventcollector.service;

import com.nashkod.avro.Device;

import java.util.List;

public interface DeduplicateService {

    List<Device> getUniqueDevices(List<Device> devices);

}
