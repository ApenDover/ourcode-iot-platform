package ts.andrey.deviceservice.data.dao;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.entity.DeviceEntity;
import ts.andrey.deviceservice.data.repository.DeviceRepository;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.exception.ErrorExceptionMessages;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDbDataService {

    private final DeviceRepository deviceRepository;
    private final EntityManager entityManager;

    @Value("${spring.application.name}")
    private String appName;

    @WithSpan
    public DeviceEntity save(DeviceEntity device) {
        device.setApplication(appName);
        final var saved = deviceRepository.save(device);
        log.info("Сохранено устройство {}", saved);
        return saved;
    }

    @WithSpan
    public int deleteByDeviceId(String deviceId) {
        return deviceRepository.deleteByDeviceId(deviceId);
    }

    @WithSpan
    public DeviceEntity getDeviceByDeviceId(String deviceId) {
        return deviceRepository.findByDeviceId(deviceId)
                .orElseThrow(() -> new DeviceServiceException(ErrorExceptionMessages.DEVICE_NOT_FOUND, deviceId));
    }

    public List<DeviceEntity> getAllDevices() {
        return deviceRepository.findAll();
    }

    public DeviceEntity updateTypeMeta(String deviceId, String deviceType, String meta) {
        deviceRepository.updateTypeAndMetaByDeviceId(deviceId, deviceType, appName, meta);
        return getDeviceByDeviceId(deviceId);
    }

    public DeviceEntity updateMeta(String deviceId, String meta) {
        deviceRepository.updateMetaByDeviceId(deviceId, appName, meta);
        return getDeviceByDeviceId(deviceId);
    }

    public DeviceEntity updateType(String deviceId, String deviceType) {
        deviceRepository.updateTypeByDeviceId(deviceId, appName, deviceType);
        return getDeviceByDeviceId(deviceId);
    }

}
