package ts.andrey.deviceservice.service;

import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;

import java.util.List;

public interface DeviceService {

    Device updateVersion(String deviceId, Long etag, String updateVersion, DeviceStatus deviceStatus);

    Device getDevice(String deviceId);

    List<Device> getDevice();

    Device createDevice(DeviceCreateRequest deviceCreateRequest);

    Device saveDevice(Device device);

    Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest);

    void deleteDevice(String deviceId);

}
