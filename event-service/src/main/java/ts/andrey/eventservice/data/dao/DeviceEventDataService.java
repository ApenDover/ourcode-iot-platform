package ts.andrey.eventservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.cassandra.core.CassandraTemplate;
import org.springframework.data.cassandra.core.query.Criteria;
import org.springframework.data.cassandra.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.repository.DeviceEventRepository;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private static final String TIMESTAMP_FIELD = "timestamp";
    private static final String TYPE_FIELD = "type";

    private final DeviceEventRepository deviceEventRepository;
    private final CassandraTemplate cassandraTemplate;

    public DeviceEventEntity getEvent(String deviceId, String eventId) {
        final var eventUuid = UUID.fromString(eventId);
        return deviceEventRepository.findByDeviceIdAndEventId(deviceId, eventUuid)
                .orElseThrow(() -> new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, deviceId, eventId));
    }

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter) {
        var query = createBaseQuery(filter);
        query = applyOptionalFilters(query, filter);
        return cassandraTemplate.select(query, DeviceEventEntity.class);
    }

    private Query createBaseQuery(EventFilterRequest filter) {
        return Query.query(Criteria.where("device_id").is(filter.getDeviceId()));
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

        query = query.limit(filter.getSize());
        query = query.sort(Sort.by(Sort.Direction.DESC, TIMESTAMP_FIELD));
        return query.withAllowFiltering();
    }

}
