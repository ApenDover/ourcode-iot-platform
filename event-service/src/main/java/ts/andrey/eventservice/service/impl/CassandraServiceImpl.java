package ts.andrey.eventservice.service.impl;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.dao.DeviceEventDataService;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;
import ts.andrey.eventservice.service.CassandraService;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CassandraServiceImpl implements CassandraService {

    private final DeviceEventDataService deviceEventDataService;
    private final EventMapper eventMapper;

    @Override
    @WithSpan("CassandraGetEvent")
    public Event getEvent(String deviceId, String eventId) {
        final var eventEntity = deviceEventDataService.getEvent(deviceId, eventId);
        return eventMapper.entityToEvent(eventEntity);
    }

    @Override
    @WithSpan("CassandraGetEventByFilter")
    public EventPage getEventByFilter(EventFilterRequest eventFilterRequest) {
        final var result = deviceEventDataService.getEventsByFilter(eventFilterRequest);
        final var filtered = filterByType(result, eventFilterRequest.getType());
        return eventMapper.entityListToEventPage(filtered, eventFilterRequest, filtered.size());
    }

    private List<DeviceEventEntity> filterByType(
            List<DeviceEventEntity> events,
            String type
    ) {
        if (type == null || type.isBlank()) {
            return events;
        }
        return events.stream()
                .filter(event ->
                        Objects.nonNull(event.getType())
                                && type.toUpperCase().equals(event.getType().name()))
                .collect(Collectors.toList());
    }

}
