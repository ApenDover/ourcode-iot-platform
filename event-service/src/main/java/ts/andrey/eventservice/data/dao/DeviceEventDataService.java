package ts.andrey.eventservice.data.dao;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventservice.data.entity.DeviceEventEntity;
import ts.andrey.eventservice.data.repository.DeviceEventRepository;
import ts.andrey.eventservice.exception.ErrorExceptionMessages;
import ts.andrey.eventservice.exception.EventServiceException;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final DeviceEventRepository deviceEventRepository;

    public DeviceEventEntity getEvent(String deviceId, UUID eventId) {
        return deviceEventRepository.findByDeviceIdAndEventId(deviceId, eventId)
                .orElseThrow(() -> new EventServiceException(ErrorExceptionMessages.EVENT_NOT_FOUND, deviceId, eventId.toString()));
    }


}
