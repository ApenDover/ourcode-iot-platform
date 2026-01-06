package ts.andrey.deviceservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ts.andrey.deviceservice.exception.ErrorExceptionMessages;
import ts.andrey.deviceservice.metrics.DeviceMetrics;
import ts.andrey.dto.Device;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceCacheDataService {

    @Value("${app.redis.ttl-minutes}")
    private int ttlMinutes;

    private final RedisTemplate<String, Device> redisTemplate;
    private final DeviceMetrics deviceMetrics;

    public Optional<Device> getDevice(String deviceId) {
        try {
            final var device = redisTemplate.opsForValue().get(deviceId);
            if (Objects.nonNull(device)) {
                log.debug("Device найден в REDIS: {}", device);
                deviceMetrics.getDeviceRedisSuccess();
                deviceMetrics.deviceRedisSuccess();
            }
            return Optional.ofNullable(device);
        } catch (Exception e) {
            deviceMetrics.deviceRedisFailure();
            log.error(ErrorExceptionMessages.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
        return Optional.empty();
    }

    public Device saveDevice(Device device) {
        try {
            redisTemplate.opsForValue()
                    .set(device.getDeviceId(), device, ttl());
            log.debug("Device сохранен в REDIS: {}", device);
            deviceMetrics.saveDeviceRedisSuccess();
            deviceMetrics.deviceRedisSuccess();
            return device;
        } catch (Exception e) {
            deviceMetrics.deviceRedisFailure();
            log.error(ErrorExceptionMessages.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
        return null;
    }

    public void deleteDevice(String deviceId) {
        try {
            redisTemplate.delete(deviceId);
            deviceMetrics.deleteDeviceRedisSuccess();
            deviceMetrics.deviceRedisSuccess();
            log.debug("Device удален из REDIS: {}", deviceId);
        } catch (Exception e) {
            deviceMetrics.deviceRedisFailure();
            log.error(ErrorExceptionMessages.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
    }

    private Duration ttl() {
        return Duration.ofMinutes(ttlMinutes);
    }

}
