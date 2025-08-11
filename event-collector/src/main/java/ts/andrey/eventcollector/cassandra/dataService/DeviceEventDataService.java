package ts.andrey.eventcollector.cassandra.dataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.repository.DeviceEventRepository;
import ts.andrey.eventcollector.exception.ExceptionMessage;
import ts.andrey.eventcollector.exception.IotException;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private static final String EVENT_ID = "eventId";

    private final DeviceEventRepository repository;

    public DeviceEventEntity save(DeviceEventEntity event) {
        final var saved = repository.save(event);
        log.info("saved event: {}", saved);
        return saved;
    }

    public DeviceEventEntity getByEventId(String eventId) {
        final var deviceEvent = repository.findDeviceEventEntityByEventId(eventId);
        if (deviceEvent.isEmpty()) {
            throw new IotException(
                    String.format(ExceptionMessage.CASSANDRA_DEVICE_EVENT_NOT_FOUND.getValue(),
                            EVENT_ID, eventId)
            );
        }
        return deviceEvent.get();
    }

    public boolean isExistDeviceId(String deviceId) {
        return repository.existsByDeviceId(deviceId);
    }

}
