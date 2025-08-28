package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.dao.DeviceDataService;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.metrics.DeviceMetrics;
import ts.andrey.deviceservice.service.DeviceCrudService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

@Service
@RequiredArgsConstructor
public class DeviceCrudServiceImpl implements DeviceCrudService {

    private final DeviceMetrics deviceMetrics;
    private final DeviceDataService deviceDataService;
    private final DeviceMapper deviceMapper;

    @Override
    public Device getDevice(String deviceId) {
        final var device = deviceDataService.getDeviceByDeviceId(deviceId);
        deviceMetrics.getDeviceSuccess();
        return deviceMapper.toDevice(device);
    }

    @Override
    public Device saveDevice(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceMapper.createDeviceEntity(deviceCreateRequest);
        final var created = deviceDataService.save(device);
        deviceMetrics.createDeviceSuccess();
        return deviceMapper.toDevice(created);
    }

    @Override
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var meta = deviceUpdateRequest.getMeta();
        final var deviceType = deviceUpdateRequest.getDeviceType();
        if (StringUtils.isNoneBlank(deviceType) && StringUtils.isNoneBlank(deviceId)) {
            final var updated = deviceDataService.updateTypeMeta(deviceId, deviceType, meta);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        if (StringUtils.isNoneBlank(deviceType)) {
            final var updated = deviceDataService.updateType(deviceId, deviceType);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        if (StringUtils.isNoneBlank(meta)) {
            final var updated = deviceDataService.updateMeta(deviceId, meta);
            deviceMetrics.updateDeviceSuccess();
            return deviceMapper.toDevice(updated);
        }
        return deviceMapper.toDevice(deviceDataService.getDeviceByDeviceId(deviceId));
    }

    @Override
    public void deleteDevice(String deviceId) {
        deviceDataService.deleteByDeviceId(deviceId);
        deviceMetrics.deleteDeviceSuccess();
    }

}
