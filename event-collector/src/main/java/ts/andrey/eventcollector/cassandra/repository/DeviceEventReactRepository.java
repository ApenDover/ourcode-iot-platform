package ts.andrey.eventcollector.cassandra.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

public interface DeviceEventReactRepository extends ReactiveCassandraRepository<DeviceEventEntity, DeviceEventKey> {

}
