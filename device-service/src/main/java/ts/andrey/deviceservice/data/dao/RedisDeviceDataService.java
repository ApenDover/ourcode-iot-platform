package ts.andrey.deviceservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import ts.andrey.deviceservice.exception.TextException;
import ts.andrey.dto.Device;

import java.time.Duration;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedisDeviceDataService {

    @Value("${app.redis.ttl-minutes}")
    private int ttlMinutes;

    private final RedisTemplate<String, Device> redisTemplate;

    public Optional<Device> getDevice(String deviceId) {
        try {
            final var device = redisTemplate.opsForValue().get(deviceId);
            log.info("Device найден в REDIS: {}", device);
            return Optional.ofNullable(device);
        } catch (Exception e) {
            log.error(TextException.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
        return Optional.empty();
    }

    public Device saveDevice(Device device) {
        try {
            redisTemplate.opsForValue()
                    .set(device.getDeviceId(), device, ttl());
            log.info("Device сохранен в REDIS: {}", device);
            return device;
        } catch (Exception e) {
            log.error(TextException.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
        return null;
    }

    public void deleteDevice(String deviceId) {
        try {
            redisTemplate.delete(deviceId);
            log.info("Device удален из REDIS: {}", deviceId);
        } catch (Exception e) {
            log.error(TextException.REDIS_NOT_AVAILABLE.format(e.getMessage()), e);
        }
    }

    private Duration ttl() {
        return Duration.ofMinutes(ttlMinutes);
    }

}
