package ts.andrey.deviceservice.metrics;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DeviceMetrics {

    private final MeterRegistry meterRegistry;

    public void getDeviceRedisSuccess() {
        meterRegistry.counter("device_service_redis_get").increment();
    }

    public void saveDeviceRedisSuccess() {
        meterRegistry.counter("device_service_redis_save").increment();
    }

    public void deleteDeviceRedisSuccess() {
        meterRegistry.counter("device_service_redis_delete").increment();
    }

    public void deviceRedisSuccess() {
        meterRegistry.counter("device_service_redis_success").increment();
    }

    public void deviceRedisFailure() {
        meterRegistry.counter("device_service_redis_fail").increment();
    }

    public void getDeviceSuccess() {
        meterRegistry.counter("device_service_database_get").increment();
    }

    public void deleteDeviceSuccess() {
        meterRegistry.counter("device_service_database_delete").increment();
    }

    public void updateDeviceSuccess() {
        meterRegistry.counter("device_service_database_update").increment();
    }

    public void createDeviceSuccess() {
        meterRegistry.counter("device_service_database_create").increment();
    }

    public void recordDatabaseError(String operation) {
        meterRegistry.counter("device_service_database_fail",
                        "operation", operation)
                .increment();
    }

}
