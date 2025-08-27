package ts.andrey.deviceservice.service;

import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

public interface DeviceCrudService {

    Device getDevice(String deviceId);

    Device saveDevice(DeviceCreateRequest deviceCreateRequest);

    Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest);

    void deleteDevice(String deviceId);

}
