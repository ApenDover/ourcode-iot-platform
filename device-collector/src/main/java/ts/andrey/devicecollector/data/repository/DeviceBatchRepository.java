package ts.andrey.devicecollector.data.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ts.andrey.devicecollector.metrics.PostgresMetrics;
import ts.andrey.devicecollector.data.entity.DeviceEntity;
import ts.andrey.devicecollector.utils.ShardUtil;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceBatchRepository {

    private static final String SQL = """
                INSERT INTO public.t_device (id, device_id, device_type, created_at, meta, version, etag)
            VALUES %s
                ON CONFLICT (device_id) DO UPDATE SET
                    device_type = EXCLUDED.device_type,
                    created_at  = EXCLUDED.created_at,
                    meta        = EXCLUDED.meta,
                    version     = EXCLUDED.version,
                    etag        = EXCLUDED.etag
            """;

    @Value("${app.shardingSphere.shardCount}")
    private Integer shardCount;

    private final PostgresMetrics postgresMetrics;
    private final JdbcTemplate jdbcTemplate;

    public int batchUpsert(List<DeviceEntity> devices) {
        if (devices == null || devices.isEmpty()) {
            return 0;
        }

        final var placeholders = devices.stream()
                .map(d -> "(?, ?, ?, ?, ?, ?, ?)")
                .collect(Collectors.joining(", "));

        final var sql = SQL.formatted(placeholders);

        final var params = new ArrayList<>();
        devices.forEach(device -> {
            params.add(Objects.requireNonNullElse(device.getId(), UUID.randomUUID()));
            params.add(device.getDeviceId());
            params.add(device.getDeviceType());
            params.add(Timestamp.from(device.getCreatedAt()));
            params.add(device.getMeta());
            params.add(device.getVersion());
            params.add(device.getEtag());
        });
        final var updated = jdbcTemplate.update(sql, params.toArray());

        log.info("сохраняю устройства: [{}]", devices.size());

        devices.forEach(d -> {
            final var shard = ShardUtil.getShardNameByString(d.getDeviceId(), shardCount);
            postgresMetrics.incrementSuccess(shard);
        });
        return updated;
    }

}
