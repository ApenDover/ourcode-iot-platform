package ts.andrey.deviceservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import ts.andrey.deviceservice.service.DeviceCrudService;
import ts.andrey.dto.Device;
import ts.andrey.dto.DeviceCreateRequest;
import ts.andrey.dto.DeviceUpdateRequest;

import java.time.Duration;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceCacheService implements DeviceCrudService {

    @Value("${app.redis.ttl-minutes}")
    private static int ttlMinutes;

    private final RedisTemplate<String, Device> redisTemplate;
    private final DeviceCrudService deviceCrudServiceImpl;

    private static final Duration TTL = Duration.ofMinutes(ttlMinutes);

    public Device getDevice(String deviceId) {
        final var cached = redisTemplate.opsForValue().get(deviceId);
        if (Objects.nonNull(cached)) {
            log.info("Объект отдан из REDIS: {}", cached.getDeviceId());
            return cached;
        }

        final var device = deviceCrudServiceImpl.getDevice(deviceId);
        redisTemplate.opsForValue()
                .set(deviceId, device, TTL);
        return device;
    }

    public Device saveDevice(DeviceCreateRequest request) {
        Device device = deviceCrudServiceImpl.saveDevice(request);
        redisTemplate.opsForValue()
                .set(device.getDeviceId(), device, TTL);
        return device;
    }

    public Device updateDevice(String deviceId, DeviceUpdateRequest request) {
        Device device = deviceCrudServiceImpl.updateDevice(deviceId, request);
        redisTemplate.opsForValue()
                .set(deviceId, device, TTL);
        return device;
    }

    public void deleteDevice(String deviceId) {
        deviceCrudServiceImpl.deleteDevice(deviceId);
        redisTemplate.delete(deviceId);
    }

}
