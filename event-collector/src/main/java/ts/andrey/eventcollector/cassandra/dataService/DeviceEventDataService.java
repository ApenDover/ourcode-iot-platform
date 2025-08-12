package ts.andrey.eventcollector.cassandra.dataService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.cassandra.entity.DeviceEventEntity;
import ts.andrey.eventcollector.cassandra.repository.DeviceEventRepository;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventDataService {

    private final DeviceEventRepository repository;

    public List<DeviceEventEntity> saveAll(List<DeviceEventEntity> events) {
        final var saved = repository.saveAll(events);
        log.debug("Сохранил события в кассандра: {}", events.size());
        return saved;
    }

    public boolean isExistDeviceId(String deviceId) {
        return repository.existsByDeviceId(deviceId);
    }

}
