package ts.andrey.devicecollector.postgres.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Repository
@RequiredArgsConstructor
public class DeviceBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public void batchUpsert(List<DeviceEntity> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }

        String sql = """
                INSERT INTO public.tdevice (id, deviceid, device_type, created_at, meta)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (deviceid) DO UPDATE SET
                    device_type = EXCLUDED.device_type,
                    created_at  = EXCLUDED.created_at,
                    meta        = EXCLUDED.meta
                """;

        final var params = devices.stream()
                .map(device -> new Object[]{
                        Objects.requireNonNullElse(device.getId(), UUID.randomUUID()),
                        device.getDeviceId(),
                        device.getDeviceType(),
                        Timestamp.from(device.getCreatedAt()),
                        device.getMeta()
                }).toList();
        jdbcTemplate.batchUpdate(sql, params);
    }

}
