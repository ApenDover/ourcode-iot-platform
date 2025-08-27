package ts.andrey.deviceservice.data.dao;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.repository.DeviceRepository;
import ts.andrey.deviceservice.exception.DeviceServiceException;
import ts.andrey.deviceservice.exception.TextException;
import ts.andrey.deviceservice.mapper.DeviceMapper;
import ts.andrey.dto.Device;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceDataService {

    private final DeviceMapper deviceMapper;
    private final DeviceRepository deviceRepository;

    public Device save(Device device) {
        final var entity = deviceMapper.toEntity(device);
        final var saved = deviceRepository.save(entity);
        log.info("Сохранено устройство {}", saved);
        return deviceMapper.toDevice(saved);
    }

    public int deleteByDeviceId(String deviceId) {
        return deviceRepository.deleteByDeviceId(deviceId);
    }

    @WithSpan
    public Device getDeviceByDeviceId(String deviceId) {
        final var device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            throw new DeviceServiceException(TextException.DEVICE_NOT_FOUND, deviceId);
        }
        return deviceMapper.toDevice(device.get());
    }


}
