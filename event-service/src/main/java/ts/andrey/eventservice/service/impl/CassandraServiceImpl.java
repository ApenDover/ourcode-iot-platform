package ts.andrey.eventservice.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.dao.DeviceEventDataService;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;
import ts.andrey.eventservice.service.CassandraService;
import ts.andrey.eventservice.utils.PageUtil;

@Slf4j
@Service
@RequiredArgsConstructor
public class CassandraServiceImpl implements CassandraService {

    private final DeviceEventDataService deviceEventDataService;
    private final EventMapper eventMapper;

    @Override
    public Event getEvent(String deviceId, String eventId) {
        final var eventEntity = deviceEventDataService.getEvent(deviceId, eventId);
        return eventMapper.entityToEvent(eventEntity);
    }

    @Override
    public EventPage getEventByFilter(EventFilterRequest eventFilterRequest) {
        final var result = deviceEventDataService.getEventsByFilter(eventFilterRequest);
        return eventMapper.entityListToEventPage(result, eventFilterRequest, result.size());
    }

}
