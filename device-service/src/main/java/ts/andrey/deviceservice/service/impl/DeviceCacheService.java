package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.dao.RedisDeviceDataService;
import ts.andrey.deviceservice.service.DeviceCrudService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheService implements DeviceCrudService {

    private final RedisDeviceDataService redisDeviceDataService;
    private final DeviceCrudService deviceCrudServiceImpl;
    
    public Device getDevice(String deviceId) {
        final var cached = redisDeviceDataService.getDevice(deviceId);
        return cached.orElseGet(() -> {
            Device device = deviceCrudServiceImpl.getDevice(deviceId);
            redisDeviceDataService.saveDevice(device);
            return device;
        });
    }

    public Device saveDevice(DeviceCreateRequest request) {
        Device device = deviceCrudServiceImpl.saveDevice(request);
        redisDeviceDataService.saveDevice(device);
        return device;
    }

    public Device updateDevice(String deviceId, DeviceUpdateRequest request) {
        Device device = deviceCrudServiceImpl.updateDevice(deviceId, request);
        redisDeviceDataService.saveDevice(device);
        return device;
    }

    public void deleteDevice(String deviceId) {
        deviceCrudServiceImpl.deleteDevice(deviceId);
        redisDeviceDataService.deleteDevice(deviceId);
    }

}
