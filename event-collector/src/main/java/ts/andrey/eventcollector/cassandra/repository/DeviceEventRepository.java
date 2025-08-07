package ts.andrey.eventcollector.cassandra.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;

import java.util.Optional;

public interface DeviceEventRepository extends CassandraRepository<DeviceEventEntity, String> {

    Optional<DeviceEventEntity> findDeviceEventEntityByEventId(String eventId);

}
