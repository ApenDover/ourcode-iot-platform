package ts.andrey.eventservice.data.repository;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Repository;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;
import ts.andrey.eventservice.data.entity.EventKeyEntity;
import ts.andrey.eventservice.data.entity.EventKeyEntityKey;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.mapper.EventMapper;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.*;

@Slf4j
@Repository
@RequiredArgsConstructor
public class CriteriaRepository {

    private final static WeakHashMap<UUID, DeviceEventKey> WEAK_HASH_MAP = new WeakHashMap<>();
    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String DEVICE_ID_FIELD = "device_id";
    private static final String TYPE_FIELD = "type";

    private final CassandraTemplate cassandraTemplate;
    private final EventMapper eventMapper;

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter, Pageable pageable) {
        Query query = Query.query(Criteria.where(DEVICE_ID_FIELD).is(filter.getDeviceId()));

        if (filter.getFromTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).gte(filter.getFromTimestamp()));
        }
        if (filter.getToTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).lte(filter.getToTimestamp()));
        }
        if (filter.getType() != null) {
            query = query.and(Criteria.where(TYPE_FIELD).is(filter.getType()));
        }

        query = query.pageRequest(pageable);

        return cassandraTemplate.select(query, DeviceEventEntity.class);
    }

    public DeviceEventEntity getEventByDeviceIdAndEventId(String deviceId, UUID eventId) {
        var hashedKey = WEAK_HASH_MAP.get(eventId);
        if (Objects.isNull(hashedKey)) {
            final var keyKey = new EventKeyEntityKey();
            keyKey.setEventId(eventId);
            final var key = Optional.ofNullable(cassandraTemplate.selectOneById(keyKey, EventKeyEntity.class))
                    .orElseThrow(() -> new EventServiceException(
                            ErrorExceptionMessages.EVENT_NOT_FOUND, eventId));
            final var eKey = eventMapper.mapFromEntityKey(key);
            WEAK_HASH_MAP.put(eventId, eKey);
            hashedKey = eKey;
        }
        return Optional.ofNullable(cassandraTemplate.selectOneById(hashedKey, DeviceEventEntity.class))
                .orElseThrow(() -> new EventServiceException(
                        ErrorExceptionMessages.EVENT_NOT_FOUND, eventId));
    }

}
