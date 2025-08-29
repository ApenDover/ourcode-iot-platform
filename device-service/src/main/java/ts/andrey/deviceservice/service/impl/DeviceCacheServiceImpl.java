package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.dao.DeviceCacheService;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheServiceImpl implements DeviceService {

    private final DeviceCacheService deviceCacheService;
    private final DeviceService deviceDataServiceImpl;

    public Device getDevice(String deviceId) {
        final var cached = deviceCacheService.getDevice(deviceId);
        return cached.orElseGet(() -> {
            Device device = deviceDataServiceImpl.getDevice(deviceId);
            deviceCacheService.saveDevice(device);
            return device;
        });
    }

    public Device saveDevice(DeviceCreateRequest request) {
        Device device = deviceDataServiceImpl.saveDevice(request);
        deviceCacheService.saveDevice(device);
        return device;
    }

    public Device updateDevice(String deviceId, DeviceUpdateRequest request) {
        Device device = deviceDataServiceImpl.updateDevice(deviceId, request);
        deviceCacheService.saveDevice(device);
        return device;
    }

    public void deleteDevice(String deviceId) {
        deviceDataServiceImpl.deleteDevice(deviceId);
        deviceCacheService.deleteDevice(deviceId);
    }

}
