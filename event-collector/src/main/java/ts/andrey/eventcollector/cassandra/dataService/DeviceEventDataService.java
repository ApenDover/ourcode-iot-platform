package ts.andrey.eventcollector.cassandra.dataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventKey;
import ts.andrey.eventcollector.cassandra.repository.DeviceEventRepository;
import ts.andrey.eventcollector.exception.ExceptionMessage;
import ts.andrey.eventcollector.exception.IotException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private static final String EVENT_ID = "eventId";

    private final DeviceEventRepository repository;

    public DeviceEventEntity save(DeviceEventEntity event) {
        final var saved = repository.save(event);
        log.debug("Сохранил событие в кассандра: {}", saved);
        return saved;
    }

    public List<DeviceEventEntity> saveAll(List<DeviceEventEntity> events) {
        final var saved = repository.saveAll(events);
        log.debug("Сохранил события в кассандра: {}", events.size());
        return saved;
    }

    public DeviceEventEntity getByEventId(DeviceEventKey key) {
        final var deviceEvent = repository.findByKeyComponents(
                key.getDeviceId(),
                key.getTimestamp(),
                key.getEventId()
        );
        if (deviceEvent.isEmpty()) {
            throw new IotException(
                    String.format(ExceptionMessage.CASSANDRA_DEVICE_EVENT_NOT_FOUND.getValue(),
                            EVENT_ID, key.getEventId())
            );
        }
        return deviceEvent.get();
    }

    public boolean isExistDeviceId(String deviceId) {
        return repository.existsByDeviceId(deviceId);
    }

}
