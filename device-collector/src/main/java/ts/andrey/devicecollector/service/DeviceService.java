package ts.andrey.devicecollector.service;

import com.nashkod.avro.Device;

import java.util.List;

public interface DeviceService {

    List<Device> createOrUpdateDevice(List<Device> devices);

}
