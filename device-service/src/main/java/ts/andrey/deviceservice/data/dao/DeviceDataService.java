package ts.andrey.deviceservice.data.dao;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.data.repository.DeviceRepository;
import ts.andrey.deviceservice.exception.DeviceServiceException;
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
        return deviceMapper.toDevice(saved);
    }

    public int deleteByDeviceId(String deviceId) {
        final var num = deviceRepository.deleteByDeviceId(deviceId);
        if (num < 1) {
            throw new DeviceServiceException("");
        }
        return num;
    }

    @WithSpan
    public Device getDeviceById(String deviceId) {
        final var device = deviceRepository.findByDeviceId(deviceId);
        if (device.isEmpty()) {
            throw new DeviceServiceException("");
        }
        return deviceMapper.toDevice(device.get());
    }

    @WithSpan
    public int updateMeta(String deviceId, String meta) {
        final var num = deviceRepository.updateMetaByDeviceId(deviceId, meta);
        if (num < 1) {
            throw new DeviceServiceException("");
        }
        return num;
    }

    @WithSpan
    public int updateType(String deviceId, String deviceType) {
        final var num = deviceRepository.updateTypeByDeviceId(deviceId, deviceType);
        if (num < 1) {
            throw new DeviceServiceException("");
        }
        return num;
    }

    @WithSpan
    public int update(String deviceId, String deviceType, String meta) {
        final var num = deviceRepository.updateTypeAndMetaByDeviceId(deviceId, deviceType, meta);
        if (num < 1) {
            throw new DeviceServiceException("");
        }
        return num;
    }


}
