package ts.andrey.eventcollector.cassandra.repository;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.data.cassandra.repository.Query;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

import java.util.List;

public interface DeviceEventRepository extends CassandraRepository<DeviceEventEntity, DeviceEventKey> {

    @Query("SELECT device_id FROM device_events WHERE device_id IN ?0")
    List<DeviceEventEntity> findExistingDeviceIds(List<String> deviceIds);

}
