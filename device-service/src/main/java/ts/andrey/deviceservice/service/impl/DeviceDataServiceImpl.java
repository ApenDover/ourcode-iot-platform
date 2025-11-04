package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.dao.DeviceDbDataService;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.metrics.DeviceMetrics;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;
import ts.andrey.dto.DeviceVersionResponse;
import ts.andrey.dto.DeviceVersionUpdateRequest;

@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceService {

    private final DeviceMetrics deviceMetrics;
    private final DeviceDbDataService deviceDbService;
    private final DeviceMapper deviceMapper;

    @Override
    public DeviceVersionResponse updateVersion(String deviceId, Long etag, String updateVersion, DeviceStatus deviceStatus) {
        final var device = deviceDbService.getDeviceByDeviceId(deviceId);
        final var oldVersion = device.getVersion();
        device.setVersion(updateVersion);
        device.setStatus(deviceStatus);
        deviceDbService.save(device);
        return deviceMapper.toUpdateVersionResponse(device, oldVersion);
    }

    @Override
    public Device getDevice(String deviceId) {
        final var device = deviceDbService.getDeviceByDeviceId(deviceId);
        deviceMetrics.getDeviceSuccess();
        return deviceMapper.toDevice(device);
    }

    @Override
    public Device createDevice(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceMapper.createDeviceEntity(deviceCreateRequest);
        final var created = deviceDbService.save(device);
        deviceMetrics.createDeviceSuccess();
        return deviceMapper.toDevice(created);
    }

    @Override
    public Device saveDevice(Device device) {
        final var deviceEntity = deviceMapper.toEntity(device);
        final var created = deviceDbService.save(deviceEntity);
        deviceMetrics.updateDeviceSuccess();
        return deviceMapper.toDevice(created);
    }

    @Override
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var meta = deviceUpdateRequest.getMeta();
        final var deviceType = deviceUpdateRequest.getDeviceType();
        if (StringUtils.isNoneBlank(deviceType) && StringUtils.isNoneBlank(meta)) {
            final var updated = deviceDbService.updateTypeMeta(deviceId, deviceType, meta);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        if (StringUtils.isNoneBlank(deviceType)) {
            final var updated = deviceDbService.updateType(deviceId, deviceType);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        if (StringUtils.isNoneBlank(meta)) {
            final var updated = deviceDbService.updateMeta(deviceId, meta);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        return deviceMapper.toDevice(deviceDbService.getDeviceByDeviceId(deviceId));
    }

    @Override
    public void deleteDevice(String deviceId) {
        deviceDbService.deleteByDeviceId(deviceId);
        deviceMetrics.deleteDeviceSuccess();
    }

}
