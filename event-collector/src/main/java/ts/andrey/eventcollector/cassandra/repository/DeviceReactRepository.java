package ts.andrey.eventcollector.cassandra.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import ts.andrey.eventcollector.cassandra.entity.DeviceEntity;

public interface DeviceReactRepository extends ReactiveCassandraRepository<DeviceEntity, String> {

}
