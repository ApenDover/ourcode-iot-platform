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
import ts.andrey.dto.DeviceVersionUpdateRequest;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheServiceImpl implements DeviceService {

    private final DeviceCacheDataService deviceCacheService;
    private final DeviceService deviceDataServiceImpl;
    private final DeviceMapper deviceMapper;

    @Override
    public DeviceVersionResponse updateVersion(String deviceId, DeviceVersionUpdateRequest request, DeviceStatus deviceStatus) {
        final var device = getDevice(deviceId);
        final var etag = device.getEtag();
        if (!etag.equals(request.getEtag())) {
            throw new DeviceServiceException(ErrorExceptionMessages.ETAG_NOT_ACTUAL);
        }
        final var oldVersion = device.getVersion();
        device.setVersion(request.getTargetVersion());
        device.setStatus(deviceStatus);
        device.setEtag(etag + 1);
        final var updated = deviceDataServiceImpl.saveDevice(device);
        deviceCacheService.saveDevice(updated);
        return deviceMapper.toUpdateVersionResponse(updated, oldVersion);
    }

    public Device getDevice(String deviceId) {
        final var cached = deviceCacheService.getDevice(deviceId);
        return cached.orElseGet(() -> {
            final var device = deviceDataServiceImpl.getDevice(deviceId);
            deviceCacheService.saveDevice(device);
            return device;
        });
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
