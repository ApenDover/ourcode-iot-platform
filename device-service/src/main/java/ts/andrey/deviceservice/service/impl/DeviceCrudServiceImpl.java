package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ts.andrey.deviceservice.data.dao.DeviceDataService;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.deviceservice.service.DeviceCrudService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

@Service
@RequiredArgsConstructor
public class DeviceCrudServiceImpl implements DeviceCrudService {

    private final DeviceDataService deviceDataService;
    private final DeviceMapper deviceMapper;

    @Override
    public Device getDevice(String deviceId) {
        final var device = deviceDataService.getDeviceByDeviceId(deviceId);
        return device;
    }

    @Override
    public Device saveDevice(DeviceCreateRequest deviceCreateRequest) {
        final var device = deviceMapper.createDevice(deviceCreateRequest);
        final var created = deviceDataService.save(device);
        return created;
    }

    @Override
    @Transactional
    public Device updateDevice(String deviceId, DeviceUpdateRequest deviceUpdateRequest) {
        final var meta = deviceUpdateRequest.getMeta();
        final var deviceType = deviceUpdateRequest.getDeviceType();
        final var updating = deviceDataService.getDeviceByDeviceId(deviceId);
        if (StringUtils.isNoneBlank(deviceType)) {
            updating.setDeviceType(deviceType);
        }
        if (StringUtils.isNoneBlank(meta)) {
            updating.setMeta(meta);
        }
        return updating;
    }

    @Override
    public void deleteDevice(String deviceId) {
        deviceDataService.deleteByDeviceId(deviceId);
    }

}
