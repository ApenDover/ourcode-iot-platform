package ts.andrey.eventcollector.data.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import ts.andrey.eventcollector.data.entity.DeviceEntity;

public interface DeviceReactRepository extends ReactiveCassandraRepository<DeviceEntity, String> {

}
