package ts.andrey.eventservice.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.openapitools.jackson.nullable.JsonNullable;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;
import ts.andrey.eventservice.data.entity.EventKeyEntity;

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
    @Mapping(target = "nextPageToken", source = "nextPageToken")
    @Mapping(target = "total", ignore = true)
    @Mapping(target = "pageNumber", ignore = true)
    @Mapping(target = "totalPages", ignore = true)
    EventPage entityListToEventPage(List<DeviceEventEntity> deviceEventEntities,
                                    String nextPageToken);

    @Mapping(target = "eventId", source = "eventKeyEntity.key.eventId")
    DeviceEventKey mapFromEntityKey(EventKeyEntity eventKeyEntity);

    default JsonNullable<String> map(String value) {
        if (value == null) {
            return JsonNullable.undefined();
        }
        return JsonNullable.of(value);
    }

}
