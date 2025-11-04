package ts.andrey.orchestrator.application.outport;

import ts.andrey.device.model.DeviceVersionResponse;
import ts.andrey.orchestrator.dto.Device;
import ts.andrey.orchestrator.dto.DeviceCreateRequest;
import ts.andrey.orchestrator.dto.DeviceUpdateRequest;

public interface DeviceServicePort {

    Device createDevice(DeviceCreateRequest deviceCreateRequest);

    Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest);

    void deleteDevice(String deviceId);

    Device getDevice(String deviceId);

    DeviceVersionResponse updateDeviceVersion(String deviceId, String deviceVersion, Long etag);

    DeviceVersionResponse rollbackDeviceVersion(String deviceId, String deviceVersion, Long etag);

}
