package ts.andrey.devicecollector.postgres.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import ts.andrey.devicecollector.metrics.GlobalMetrics;
import ts.andrey.devicecollector.metrics.PostgresMetrics;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;
import ts.andrey.devicecollector.utils.ShardUtil;

import java.sql.BatchUpdateException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class DeviceBatchRepository {

    private static final String SQL = """
                INSERT INTO public.t_device (id, device_id, device_type, created_at, meta)
                VALUES (?, ?, ?, ?, ?)
                ON CONFLICT (device_id) DO UPDATE SET
                    device_type = EXCLUDED.device_type,
                    created_at  = EXCLUDED.created_at,
                    meta        = EXCLUDED.meta
            """;

    private final PostgresMetrics postgresMetrics;
    private final GlobalMetrics globalMetrics;
    private final JdbcTemplate jdbcTemplate;

    public void batchUpsert(List<DeviceEntity> devices) {
        if (devices == null || devices.isEmpty()) {
            return;
        }

        try {
            jdbcTemplate.batchUpdate(SQL, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    final var device = devices.get(i);
                    ps.setObject(1, Objects.requireNonNullElse(device.getId(), UUID.randomUUID()));
                    ps.setString(2, device.getDeviceId());
                    ps.setString(3, device.getDeviceType());
                    ps.setTimestamp(4, Timestamp.from(device.getCreatedAt()));
                    ps.setString(5, device.getMeta());
                }

                @Override
                public int getBatchSize() {
                    return devices.size();
                }
            });

            devices.forEach(d -> {
                final var shard = ShardUtil.getShardNameByString(d.getDeviceId());
                postgresMetrics.incrementSuccess(shard);
                globalMetrics.incrementSuccess();
            });

        } catch (DataAccessException e) {
            if (e.getCause() instanceof BatchUpdateException ex) {
                final var updateCounts = ex.getUpdateCounts();
                for (int i = 0; i < updateCounts.length; i++) {
                    if (updateCounts[i] == Statement.EXECUTE_FAILED) {
                        log.error("DeviceId {} не сохранен в базу данных: ", devices.get(i).getDeviceId(), e);
                        String shard = ShardUtil.getShardNameByString(devices.get(i).getDeviceId());
                        postgresMetrics.incrementError(shard);
                    } else {
                        String shard = ShardUtil.getShardNameByString(devices.get(i).getDeviceId());
                        postgresMetrics.incrementSuccess(shard);
                    }
                }
            } else {
                log.error("Batch({}) с Device не сохранен в базу данных: ", devices.size(), e);
                devices.forEach(d -> {
                    final var shard = ShardUtil.getShardNameByString(d.getDeviceId());
                    postgresMetrics.incrementError(shard);
                });
            }
        }
    }

}
