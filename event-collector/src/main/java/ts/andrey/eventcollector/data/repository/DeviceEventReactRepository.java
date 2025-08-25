package ts.andrey.eventcollector.data.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import ts.andrey.eventcollector.data.entity.DeviceEventEntity;
import ts.andrey.eventcollector.data.entity.DeviceEventKey;

public interface DeviceEventReactRepository extends ReactiveCassandraRepository<DeviceEventEntity, DeviceEventKey> {

}
