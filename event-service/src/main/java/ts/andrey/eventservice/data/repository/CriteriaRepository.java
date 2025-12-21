package ts.andrey.eventservice.data.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CriteriaRepository {

    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String DEVICE_ID_FIELD = "device_id";

    private final CassandraTemplate cassandraTemplate;

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter, Pageable pageable) {
        Query query = Query.query(Criteria.where(DEVICE_ID_FIELD).is(filter.getDeviceId()));

        if (filter.getFromTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).gte(filter.getFromTimestamp()));
        }
        if (filter.getToTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).lte(filter.getToTimestamp()));
        }

        query = query.pageRequest(pageable);

        List<DeviceEventEntity> events = cassandraTemplate.select(query, DeviceEventEntity.class);

        if (Objects.nonNull(filter.getType())) {
            events = events.stream()
                    .filter(event -> filter.getType().equals(event.getType().toString()))
                    .toList();
        }

        return events;
    }

    public DeviceEventEntity getEventByDeviceIdAndEventId(String deviceId, UUID eventId) {
        final var key = new DeviceEventKey();
        key.setDeviceId(deviceId);
        key.setEventId(eventId);

        final var entity = cassandraTemplate.selectOneById(key, DeviceEventEntity.class);

        if (Objects.isNull(entity)) {
            throw new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, eventId);
        }
        return entity;
    }

}
