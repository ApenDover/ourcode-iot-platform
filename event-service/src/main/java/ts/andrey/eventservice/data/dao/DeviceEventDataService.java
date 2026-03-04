package ts.andrey.eventservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.repository.CriteriaRepository;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final CriteriaRepository criteriaRepository;

    public DeviceEventEntity getEvent(String deviceId, String eventId) {
        final var eventUuid = UUID.fromString(eventId);
        final var event = criteriaRepository.getEventByDeviceIdAndEventId(deviceId, eventUuid);
        if (!event.getKey().getDeviceId().equals(deviceId)) {
            throw new EventServiceException(ErrorExceptionMessages.EVENT_DEVICE_NOT_FOUND, deviceId, eventId);
        }
        return event;
    }

    public Slice<DeviceEventEntity> getEventsSlice(EventFilterRequest filter, Pageable pageable) {
        return criteriaRepository.getEventsSlice(filter, pageable);
    }

    public long countEventsByFilter(EventFilterRequest filter) {
        return criteriaRepository.countEventsByFilter(filter);
    }

}
