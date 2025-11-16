package ts.andrey.deviceservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.api.DeviceV1Api;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;
import ts.andrey.dto.DeviceVersionResponse;
import ts.andrey.dto.DeviceVersionRollbackRequest;
import ts.andrey.dto.DeviceVersionUpdateRequest;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceV1Api {

    private final DeviceService deviceService;

    @Override
    public ResponseEntity<DeviceVersionResponse> updateDeviceVersion(
            String deviceId, DeviceVersionUpdateRequest deviceVersionUpdateRequest
    ) {
        final var response = deviceService.updateVersion(
                deviceId,
                deviceVersionUpdateRequest.getEtag(),
                deviceVersionUpdateRequest.getTargetVersion(),
                DeviceStatus.UPDATING);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<DeviceVersionResponse> rollbackDeviceVersion(String deviceId, DeviceVersionRollbackRequest deviceVersionRollbackRequest) {
        final var response = deviceService.updateVersion(
                deviceId,
                deviceVersionRollbackRequest.getEtag(),
                deviceVersionRollbackRequest.getRollbackVersion(),
                DeviceStatus.READY);
        return ResponseEntity.ok(response);
    }

    @Override
    public ResponseEntity<Device> createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceService.createDevice(deviceCreateRequest);
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<Void> deleteDevice(String deviceId) {
        deviceService.deleteDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Device> getDevice(String deviceId) {
        final var device = deviceService.getDevice(deviceId);
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<List<Device>> getDevices() {
        final var device = deviceService.getDevice();
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<Device> updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var device = deviceService.updateDevice(deviceId, deviceUpdateRequest);
        return ResponseEntity.ok(device);
    }

}
