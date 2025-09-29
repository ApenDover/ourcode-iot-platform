package ts.andrey.eventservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(target = "eventId", source = "deviceEventEntity.key.eventId")
    @Mapping(target = "deviceId", source = "deviceEventEntity.key.deviceId")
    @Mapping(target = "timestamp", source = "deviceEventEntity.key.timestamp")
    @Mapping(target = "type", expression = "java(deviceEventEntity.getType().toString())")
    @Mapping(target = "payload", source = "payload")
    Event entityToEvent(DeviceEventEntity deviceEventEntity);

    List<Event> entityListToEventList(List<DeviceEventEntity> deviceEventEntity);

    @Mapping(target = "events", expression = "java(entityListToEventList(deviceEventEntities))")
    @Mapping(target = "page", source = "eventFilterRequest.page")
    @Mapping(target = "size", source = "eventFilterRequest.size")
    @Mapping(target = "total", source = "total")
    EventPage entityListToEventPage(List<DeviceEventEntity> deviceEventEntities, EventFilterRequest eventFilterRequest, Integer total);

}
