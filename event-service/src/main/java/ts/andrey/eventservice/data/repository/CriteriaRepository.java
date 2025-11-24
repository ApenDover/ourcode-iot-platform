package ts.andrey.eventservice.data.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.stereotype.Component;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class CriteriaRepository {

    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String TYPE_FIELD = "type";

    private final CassandraTemplate cassandraTemplate;

    public DeviceEventEntity getEventById(UUID eventId) {
        final var query = Query.query(Criteria.where("event_id")
                        .is(eventId))
                .withAllowFiltering();
        final var list = cassandraTemplate.select(query, DeviceEventEntity.class);
        if (list.isEmpty()) {
            throw new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, eventId);
        }
        return list.get(0);
    }

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter) {
        var query = Query.query(Criteria.where("device_id")
                .is(filter.getDeviceId()));
        query = applyOptionalFilters(query, filter);
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
        return query.withAllowFiltering();
    }

}
