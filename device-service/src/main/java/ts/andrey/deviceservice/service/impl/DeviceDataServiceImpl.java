package ts.andrey.deviceservice.service.impl;

import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ts.andrey.deviceservice.data.dao.DeviceDbDataService;
import ts.andrey.deviceservice.data.repository.DeviceRepository;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.metrics.DeviceMetrics;
import ts.andrey.deviceservice.service.DeviceService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceUpdateRequest;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceDataServiceImpl implements DeviceService {

    @Value("${spring.application.name}")
    private String appName;

    private final DeviceMetrics deviceMetrics;
    private final DeviceDbDataService deviceDbService;
    private final DeviceMapper deviceMapper;
    private final DeviceRepository deviceRepository;
    private final EntityManager entityManager;

    @Override
    public Device getDevice(String deviceId) {
        final var device = deviceDbService.getDeviceByDeviceId(deviceId);
        deviceMetrics.getDeviceSuccess();
        return deviceMapper.toDevice(device);
    }

    @Override
    public List<Device> getDevice() {
        final var devices = deviceDbService.getAllDevices();
        deviceMetrics.getDeviceSuccess();
        return deviceMapper.toDevices(devices);
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
    public Device updateVersion(String deviceId, Long etag, String updateVersion, DeviceStatus deviceStatus) {
        final var device = deviceDbService.getDeviceByDeviceId(deviceId);
        device.setVersion(updateVersion);
        device.setStatus(deviceStatus);
        device.setEtag(etag+1);
        final var updated = deviceDbService.saveAndrey(device);
        return deviceMapper.toDevice(updated);
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
