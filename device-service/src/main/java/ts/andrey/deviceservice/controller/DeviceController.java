package ts.andrey.deviceservice.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.api.DeviceV1Api;
import ts.andrey.deviceservice.service.DeviceCrudService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

@RestController
@RequiredArgsConstructor
public class DeviceController implements DeviceV1Api {

    private final DeviceCrudService deviceCrudService;

    @Override
    public ResponseEntity<Device> createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceCrudService.saveDevice(deviceCreateRequest);
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<Void> deleteDevice(String deviceId) {
        deviceCrudService.deleteDevice(deviceId);
        return ResponseEntity.ok().build();
    }

    @Override
    public ResponseEntity<Device> getDevice(String deviceId) {
        final var device = deviceCrudService.getDevice(deviceId);
        return ResponseEntity.ok(device);
    }

    @Override
    public ResponseEntity<Device> updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var device = deviceCrudService.updateDevice(deviceId, deviceUpdateRequest);
        return ResponseEntity.ok(device);
    }

}
