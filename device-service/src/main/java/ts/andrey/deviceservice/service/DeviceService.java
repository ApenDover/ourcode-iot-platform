package ts.andrey.deviceservice.service;

import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;
import ts.andrey.dto.DeviceVersionResponse;
import ts.andrey.dto.DeviceVersionUpdateRequest;

public interface DeviceService {

    DeviceVersionResponse updateVersion(String deviceId, DeviceVersionUpdateRequest request, DeviceStatus deviceStatus);

    Device getDevice(String deviceId);

    Device createDevice(DeviceCreateRequest deviceCreateRequest);

    Device saveDevice(Device device);

    Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest);

    void deleteDevice(String deviceId);

}
