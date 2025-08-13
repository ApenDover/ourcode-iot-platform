package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventService {

    private final DeviceEventDataService deviceEventDataService;
    private final SimpleCache simpleCache;
    private final DeviceEventMapper deviceEventMapper;

    /**
     * Сохраняем только те события в cassandra, deviceId которых есть в simpleCache
     * @param events набор событий
     * @return не сохраненные события
     */
    public List<DeviceEvent> saveCashedEvents(List<DeviceEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return List.of();
        }
        final var cachedDeviceEvent = events.stream()
                .filter(it -> simpleCache.contains(it.getDeviceId()))
                .toList();

        final var cashedDeviceEventEntities = deviceEventMapper.toEntityList(cachedDeviceEvent);
        deviceEventDataService.saveAll(cashedDeviceEventEntities);

        final var modifiableEvents = new ArrayList<>(events);
        modifiableEvents.removeAll(cachedDeviceEvent);
        return modifiableEvents;
    }

    public void saveEvents(List<DeviceEvent> events) {
        final var toSave = deviceEventMapper.toEntityList(events);
        deviceEventDataService.saveAll(toSave);
    }

}
