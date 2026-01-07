package ts.andrey.eventcollector.data.repository;

import org.springframework.data.cassandra.repository.ReactiveCassandraRepository;
import ts.andrey.eventcollector.data.entity.DeviceEventEntity;
import ts.andrey.eventcollector.data.entity.DeviceEventKey;
import ts.andrey.eventcollector.data.entity.EventKeyEntity;
import ts.andrey.eventcollector.data.entity.EventKeyEntityKey;

public interface DeviceEventKeyReactRepository extends ReactiveCassandraRepository<EventKeyEntity, EventKeyEntityKey> {

}
