package ts.andrey.deviceservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.dto.DeviceStatus;
import ts.andrey.dto.DeviceVersionResponse;
import ts.andrey.dto.DeviceVersionRollbackRequest;
import ts.andrey.dto.DeviceVersionUpdateRequest;

@Slf4j
@RequiredArgsConstructor
@Component
public class SagaService {

    private final DeviceService deviceCacheServiceImpl;
    private final DeviceMapper deviceMapper;

    public DeviceVersionResponse updateVersion(String deviceId, DeviceVersionUpdateRequest deviceVersionUpdateRequest) {
        final var actualDevice = deviceCacheServiceImpl.getDevice(deviceId);
        if (!actualDevice.getEtag().equals(deviceVersionUpdateRequest.getEtag())) {
            throw new DeviceServiceException("Etag не совпал, попробуй еще раз");
        }
        if (DeviceStatus.UPDATING.equals(actualDevice.getStatus())) {
            throw new DeviceServiceException("Ошибка обновления: данное устройство уже в работе");
        }
        final var updatedDevice = deviceCacheServiceImpl.updateVersion(deviceId,
                deviceVersionUpdateRequest.getEtag(),
                deviceVersionUpdateRequest.getTargetVersion(),
                DeviceStatus.UPDATING);
        return deviceMapper.toUpdateVersionResponse(updatedDevice, actualDevice.getVersion());
    }

    public DeviceVersionResponse rollbackVersion(String deviceId, DeviceVersionRollbackRequest deviceVersionRollbackRequest) {
        final var actualDevice = deviceCacheServiceImpl.getDevice(deviceId);
        if (DeviceStatus.READY.equals(actualDevice.getStatus())) {
            throw new DeviceServiceException("Ошибка восстановления: данное устройство в статусе READY");
        }
        final var updatedDevice = deviceCacheServiceImpl.updateVersion(deviceId,
                deviceVersionRollbackRequest.getEtag(),
                deviceVersionRollbackRequest.getRollbackVersion(),
                DeviceStatus.READY);
        return deviceMapper.toUpdateVersionResponse(updatedDevice, actualDevice.getVersion());
    }

}
