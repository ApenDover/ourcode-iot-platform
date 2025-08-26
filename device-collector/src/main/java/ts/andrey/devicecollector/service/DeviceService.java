package ts.andrey.devicecollector.service;

import com.nashkod.avro.Device;

import java.util.List;

public interface DeviceService {

    void createOrUpdateDevice(List<Device> devices);

}
