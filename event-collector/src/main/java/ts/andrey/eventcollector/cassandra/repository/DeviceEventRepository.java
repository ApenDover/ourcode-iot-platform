package ts.andrey.eventcollector.cassandra.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

import java.util.Optional;
import java.util.UUID;

public interface DeviceEventRepository extends CassandraRepository<DeviceEventEntity, DeviceEventKey> {

    @Query("SELECT * FROM device_events WHERE device_id = ?0 AND timestamp = ?1 AND event_id = ?2")
    Optional<DeviceEventEntity> findByKeyComponents(String deviceId, long timestamp, UUID eventId);

    @Query("SELECT * FROM device_events WHERE device_id = ?0 LIMIT 1")
    Optional<DeviceEventEntity> findFirstByDeviceId(String deviceId);

    default boolean existsByDeviceId(String deviceId) {
        return findFirstByDeviceId(deviceId).isPresent();
    }

}
