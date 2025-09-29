package ts.andrey.eventservice.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.dao.DeviceEventDataService;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;
import ts.andrey.eventservice.service.CassandraService;
import ts.andrey.eventservice.utils.PageUtil;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CassandraServiceImpl implements CassandraService {

    private final DeviceEventDataService deviceEventDataService;
    private final EventMapper eventMapper;

    @Override
    public Event getEvent(String deviceId, String eventId) {
        final var eventIdUuid = UUID.fromString(eventId);
        final var eventEntity = deviceEventDataService.getEvent(deviceId, eventIdUuid);
        return eventMapper.entityToEvent(eventEntity);
    }

    @Override
    public EventPage getEventByFilter(EventFilterRequest eventFilterRequest) {
        final var result = deviceEventDataService.getEventsByFilter(eventFilterRequest);
        final var list = PageUtil.getPageableList(result, eventFilterRequest);
        return eventMapper.entityListToEventPage(list, eventFilterRequest, result.size());
    }

}
