package ts.andrey.eventservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.repository.CriteriaRepository;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;
import ts.andrey.eventservice.model.EventFilterRequest;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final CriteriaRepository criteriaRepository;

    public DeviceEventEntity getEvent(String deviceId, String eventId) {
        final var eventUuid = UUID.fromString(eventId);
        final var event = criteriaRepository.getEventById(eventUuid);
        if (!event.getKey().getDeviceId().equals(deviceId)) {
            throw new EventServiceException(ErrorExceptionMessages.EVENT_DEVICE_NOT_FOUND, deviceId, eventId);
        }
        return event;
    }

    public List<DeviceEventEntity> getEventsByFilter(EventFilterRequest filter) {
        return criteriaRepository.getEventsByFilter(filter);
    }

}
