package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.dao.DeviceCacheDataService;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.exception.ErrorExceptionMessages;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;
import ts.andrey.dto.DeviceVersionResponse;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheServiceImpl implements DeviceService {

    private final DeviceCacheDataService deviceCacheService;
    private final DeviceService deviceDataServiceImpl;

    @Override
    public Device updateVersion(String deviceId, Long etag, String updateVersion, DeviceStatus deviceStatus) {
        final var updated = deviceDataServiceImpl.updateVersion(deviceId, etag, updateVersion, deviceStatus);
        saveDevice(updated);
        return updated;
    }

    public Device getDevice(String deviceId) {
        final var cached = deviceCacheService.getDevice(deviceId);
        return cached.orElseGet(() -> {
            final var device = deviceDataServiceImpl.getDevice(deviceId);
            deviceCacheService.saveDevice(device);
            return device;
        });
    }

    public List<Device> getDevice() {
        return deviceDataServiceImpl.getDevice();
    }

    public Device createDevice(DeviceCreateRequest request) {
        final var device = deviceDataServiceImpl.createDevice(request);
        deviceCacheService.saveDevice(device);
        return device;
    }

    public Device saveDevice(Device device) {
        return deviceCacheService.saveDevice(device);
    }

    public Device updateDevice(String deviceId, DeviceUpdateRequest request) {
        final var device = deviceDataServiceImpl.updateDevice(deviceId, request);
        deviceCacheService.saveDevice(device);
        return device;
    }

    public void deleteDevice(String deviceId) {
        deviceDataServiceImpl.deleteDevice(deviceId);
        deviceCacheService.deleteDevice(deviceId);
    }

}
