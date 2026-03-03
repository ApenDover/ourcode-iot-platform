package ts.andrey.eventservice.service.impl;

import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.cassandra.core.query.CassandraPageRequest;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import ts.andrey.dto.Event;
import ts.andrey.dto.EventPage;
import ts.andrey.eventservice.data.dao.DeviceEventDataService;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;
import ts.andrey.eventservice.service.CassandraService;

import java.nio.ByteBuffer;
import java.util.Base64;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CassandraServiceImpl implements CassandraService {

    private final DeviceEventDataService deviceEventDataService;

    private final EventMapper eventMapper;

    @Value("${app.events.page-size:100}")
    private int pageSize;

    @Override
    @WithSpan("CassandraGetEvent")
    public Event getEvent(String deviceId, String eventId) {
        final var eventEntity = deviceEventDataService.getEvent(deviceId, eventId);
        return eventMapper.entityToEvent(eventEntity);
    }

    @Override
    @WithSpan("CassandraGetEventByFilter")
    public EventPage getEventByFilter(EventFilterRequest eventFilterRequest) {
        final var pagingState = decodeToken(eventFilterRequest.getPageToken());
        final var pageRequest = buildPageRequest(pageSize, pagingState);
        final var slice = deviceEventDataService.getEventsSlice(eventFilterRequest, pageRequest);
        final var filtered = filterByType(slice.getContent(), eventFilterRequest.getType());
        final var nextToken = encodeToken(extractPagingState(slice));
        return eventMapper.entityListToEventPage(filtered, nextToken);
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

    private CassandraPageRequest buildPageRequest(int size, ByteBuffer pagingState) {
        if (pagingState == null) {
            return CassandraPageRequest.first(size);
        }
        return CassandraPageRequest.of(PageRequest.of(0, size), pagingState);
    }

    private ByteBuffer extractPagingState(Slice<DeviceEventEntity> slice) {
        if (slice == null) {
            return null;
        }
        if (!(slice.getPageable() instanceof CassandraPageRequest cassandraPageRequest)) {
            return null;
        }
        return cassandraPageRequest.getPagingState();
    }

    private String encodeToken(ByteBuffer pagingState) {
        if (pagingState == null) {
            return null;
        }
        byte[] bytes = new byte[pagingState.remaining()];
        pagingState.duplicate().get(bytes);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private ByteBuffer decodeToken(String token) {
        if (token == null || token.isBlank()) {
            return null;
        }
        try {
            return ByteBuffer.wrap(Base64.getDecoder().decode(token));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid paging state token; ignoring.");
            return null;
        }
    }

}
