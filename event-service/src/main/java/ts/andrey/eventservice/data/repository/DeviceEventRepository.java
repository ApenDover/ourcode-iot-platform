package ts.andrey.eventservice.data.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import org.springframework.data.domain.Pageable;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DeviceEventRepository extends CassandraRepository<DeviceEventEntity, DeviceEventKey> {


    @Query("SELECT * FROM device_events WHERE device_id = ?0 AND event_id = ?1")
    Optional<DeviceEventEntity> findByDeviceIdAndEventId(String deviceId, UUID eventId);

    @Query("SELECT * FROM device_events WHERE device_id = ?0 AND timestamp >= ?1 AND timestamp <= ?2 AND type = ?3 ALLOW FILTERING")
    List<DeviceEventEntity> findByDeviceIdAndTimestampBetweenAndType(
            String deviceId, Long fromTimestamp, Long toTimestamp, String type, Pageable pageable);

    @Query("SELECT * FROM device_events WHERE device_id = ?0 AND timestamp >= ?1 AND timestamp <= ?2 ALLOW FILTERING")
    List<DeviceEventEntity> findByDeviceIdAndTimestampBetween(
            String deviceId, Long fromTimestamp, Long toTimestamp, Pageable pageable);

    @Query("SELECT * FROM device_events WHERE device_id = ?0 ALLOW FILTERING")
    List<DeviceEventEntity> findByDeviceId(String deviceId, Pageable pageable);
}
