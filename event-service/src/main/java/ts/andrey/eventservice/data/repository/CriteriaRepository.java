package ts.andrey.eventservice.data.repository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;
import ts.andrey.eventservice.data.entity.EventKeyEntity;
import ts.andrey.eventservice.data.entity.EventKeyEntityKey;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CriteriaRepository {

    private static final ConcurrentMap<UUID, DeviceEventKey> EVENT_KEY_CACHE = new ConcurrentHashMap<>();
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String DEVICE_ID_FIELD = "device_id";

    private final CassandraTemplate cassandraTemplate;
    private final EventMapper eventMapper;

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter, Pageable pageable) {
        var query = buildBaseQuery(filter);
        query = query.pageRequest(pageable);
        return cassandraTemplate.select(query, DeviceEventEntity.class);
    }

    public Slice<DeviceEventEntity> getEventsSlice(EventFilterRequest filter, Pageable pageable) {
        var query = buildBaseQuery(filter);
        query = query.pageRequest(pageable);
        return cassandraTemplate.slice(query, DeviceEventEntity.class);
    }

    public long countEventsByFilter(EventFilterRequest filter) {
        var query = buildBaseQuery(filter);
        return cassandraTemplate.count(query, DeviceEventEntity.class);
    }

    private Query buildBaseQuery(EventFilterRequest filter) {
        var query = Query.query(Criteria.where(DEVICE_ID_FIELD).is(filter.getDeviceId()));
        if (filter.getFromTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).gte(filter.getFromTimestamp()));
        }
        if (filter.getToTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).lte(filter.getToTimestamp()));
        }
        query = query.sort(Sort.by(Sort.Order.desc(TIMESTAMP_FIELD)));
        return query;
    }

    public DeviceEventEntity getEventByDeviceIdAndEventId(String deviceId, UUID eventId) {
        var hashedKey = EVENT_KEY_CACHE.get(eventId);
        if (Objects.isNull(hashedKey)) {
            final var keyKey = new EventKeyEntityKey();
            keyKey.setEventId(eventId);
            final var key = Optional.ofNullable(cassandraTemplate.selectOneById(keyKey, EventKeyEntity.class))
                    .orElseThrow(() -> new EventServiceException(
                            ErrorExceptionMessages.EVENT_NOT_FOUND, eventId));
            final var eKey = eventMapper.mapFromEntityKey(key);
            EVENT_KEY_CACHE.put(eventId, eKey);
            hashedKey = eKey;
        }
        return Optional.ofNullable(cassandraTemplate.selectOneById(hashedKey, DeviceEventEntity.class))
                .orElseThrow(() -> new EventServiceException(
                        ErrorExceptionMessages.EVENT_NOT_FOUND, eventId));
    }

}
