package ts.andrey.eventcollector.mapper;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.eventcollector.cassandra.entity.DeviceEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;

import java.util.List;

@Mapper(componentModel = "spring")
public interface DeviceEventMapper {

    @Mapping(target = "key", expression = "java(eventToEntityKey(deviceEvent))")
    DeviceEventEntity eventToEntity(DeviceEvent deviceEvent);

    List<DeviceEventEntity> eventToEntityList(List<DeviceEvent> deviceEvent);

    @Mapping(target = "deviceId", source = "deviceEvent.device.deviceId")
    @Mapping(target = "timestamp", expression = "java(deviceEvent.getTimestamp().toEpochMilli())")
    DeviceEventKey eventToEntityKey(DeviceEvent deviceEvent);

    DeviceEntity deviceToEntity(Device device);

    List<DeviceEntity> deviceToEntityList(List<Device> device);

}
