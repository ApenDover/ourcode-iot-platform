package ts.andrey.eventcollector.mapper;

import com.nashkod.avro.DeviceEvent;
import org.mapstruct.Mapper;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;

@Mapper(componentModel = "spring")
public interface DeviceEventMapper {

    DeviceEventEntity toEntity(DeviceEvent deviceEvent);

}
