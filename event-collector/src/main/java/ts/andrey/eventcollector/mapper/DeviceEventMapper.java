package ts.andrey.eventcollector.mapper;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.Device;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

@Mapper(componentModel = "spring")
public interface DeviceEventMapper {

    @Mapping(target = "key", expression = "java(toEntityKey(deviceEvent))")
    DeviceEventEntity toEntity(DeviceEvent deviceEvent);

    DeviceEventKey toEntityKey(DeviceEvent deviceEvent);

    Device toDeviceId(DeviceEvent deviceEvent);

}
