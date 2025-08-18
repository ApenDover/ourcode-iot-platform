package ts.andrey.devicecollector.postgres.repository;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ts.andrey.devicecollector.postgres.entity.DeviceEntity;

import java.sql.Array;
import java.sql.Connection;
import java.sql.Timestamp;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Repository
public class DeviceBatchRepository {

    private final JdbcTemplate jdbcTemplate;

    public DeviceBatchRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Transactional
    public void batchUpsert(List<DeviceEntity> devices) {
        final var incomingIds = devices.stream()
                .map(DeviceEntity::getDeviceId)
                .collect(Collectors.toSet());

        final var existingDevices = jdbcTemplate.query(
                        "SELECT device_id, version FROM t_device WHERE device_id IN ("
                                + incomingIds.stream().map(id -> "?")
                                .collect(Collectors.joining(",")) + ")",
                        incomingIds.toArray(),
                        (rs, rowNum) -> new AbstractMap.SimpleEntry<>(
                                rs.getString("device_id").trim(),
                                rs.getLong("version")
                        )
                ).stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey, Map.Entry::getValue)
                );

        final var newDevices = new ArrayList<DeviceEntity>();
        final var devicesToUpdate = new ArrayList<DeviceEntity>();

        devices.forEach(device -> {
            if (existingDevices.containsKey(device.getDeviceId())) {
                device.setVersion(existingDevices.get(device.getDeviceId()));
                devicesToUpdate.add(device);
            } else {
                newDevices.add(device);
            }
        });

        if (!newDevices.isEmpty()) {
            batchInsert(newDevices);
        }

        if (!devicesToUpdate.isEmpty()) {
            batchUpdate(devicesToUpdate);
        }

    }

    private void batchInsert(List<DeviceEntity> newDevices) {
        jdbcTemplate.update(
                """
                        INSERT INTO t_device (device_id, device_type, created_at, meta, version)
                        SELECT * FROM UNNEST(?::text[], ?::text[], ?::timestamp[], ?::text[], ?::bigint[])
                        """,
                createArray("text", newDevices, DeviceEntity::getDeviceId),
                createArray("text", newDevices, DeviceEntity::getDeviceType),
                createArray("timestamp", newDevices, d -> Timestamp.from(d.getCreatedAt())),
                createArray("text", newDevices, DeviceEntity::getMeta),
                createArray("bigint", newDevices, d -> 1L) // Начальная версия = 1
        );
    }

    private void batchUpdate(List<DeviceEntity> devicesToUpdate) {
        final var updateCounts = jdbcTemplate.batchUpdate(
                """
                        UPDATE t_device
                        SET device_type = ?,
                            meta = ?,
                            created_at = ?,
                            version = version + 1
                        WHERE device_id = ? AND version = ?
                        """,
                devicesToUpdate.stream()
                        .map(device -> new Object[]{
                                device.getDeviceType(),
                                device.getMeta(),
                                Timestamp.from(device.getCreatedAt()),
                                device.getDeviceId(),
                                device.getVersion()
                        }).toList()
        );

        // Проверяем конфликты версий
        final long failedUpdates = Arrays.stream(updateCounts)
                .filter(count -> count == 0)
                .count();
        if (failedUpdates > 0) {
            throw new OptimisticLockingFailureException(failedUpdates + " records were modified concurrently");
        }
    }

    private Array createArray(String pgType, List<DeviceEntity> devices, Function<DeviceEntity, ?> mapper) {
        return jdbcTemplate.execute((Connection conn) ->
                conn.createArrayOf(pgType, devices.stream()
                        .map(mapper)
                        .toArray())
        );
    }

}
