package ts.andrey.eventservice.data.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.ReactiveCassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.entity.DeviceEventKey;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CriteriaRepository {

    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String TYPE_FIELD = "type";

    private final ReactiveCassandraTemplate cassandraTemplate;

    // Добавьте в DeviceEventEntity, если нужен фильтр по type
    // @Indexed(name = "device_events_type_idx")
    // private EventType type;  // Если добавите, Cassandra создаст index (overhead на запись ~5%)

    public Mono<DeviceEventEntity> getEventById(UUID eventId, String deviceId, Long timestamp) {
        DeviceEventKey key = new DeviceEventKey();
        key.setDeviceId(deviceId);
        key.setEventId(eventId);
        key.setTimestamp(timestamp);  // Если timestamp известен; иначе, если нет, используйте query ниже

        return cassandraTemplate.selectOneById(key, DeviceEventEntity.class)
                .switchIfEmpty(Mono.error(new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, eventId)));
    }

    // Альтернатива, если timestamp не известен (сканирует кластер по device_id + event_id)
    public Mono<DeviceEventEntity> getEventByIdWithoutTimestamp(UUID eventId, String deviceId) {
        Query query = Query.query(
                Criteria.where("device_id").is(deviceId),
                Criteria.where("event_id").is(eventId)
        ).limit(1).sort(Sort.by(Sort.Direction.DESC, TIMESTAMP_FIELD));

        return cassandraTemplate.selectOne(query, DeviceEventEntity.class)
                .switchIfEmpty(Mono.error(new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, eventId)));
    }

    public Flux<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter, Pageable pageable) {
        Query query = Query.query(Criteria.where("device_id").is(filter.getDeviceId()));
        query = applyOptionalFilters(query, filter);

        query = query.pageRequest(pageable);  // Пагинация: e.g., PageRequest.of(0, 50, Sort.by(Sort.Direction.DESC, TIMESTAMP_FIELD))

        return cassandraTemplate.select(query, DeviceEventEntity.class);
    }

    private Query applyOptionalFilters(Query query, EventFilterRequest filter) {
        if (filter.getFromTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).gte(filter.getFromTimestamp()));
        }
        if (filter.getToTimestamp() != null) {
            query = query.and(Criteria.where(TIMESTAMP_FIELD).lte(filter.getToTimestamp()));
        }
        if (filter.getType() != null) {
            query = query.and(Criteria.where(TYPE_FIELD).is(filter.getType()));
        }
        return query;  // Без allowFiltering — если type без index, добавьте @Indexed, иначе передизайньте модель (отдельная таблица по type)
    }

}
