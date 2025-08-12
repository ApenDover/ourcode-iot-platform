package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
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

    /***
     * Сохраняем события в cassandra deviceId которых есть в simpleCache
     * @param events - весь набор событий
     * @return - список событий с deviceId, которых нет в simpleCache
     */
    public List<DeviceEvent> saveCashedEvents(List<DeviceEvent> events) {
        final var modifiableEvents = new ArrayList<>(events);
        final var cachedDeviceEvent = events.stream()
                .filter(it -> simpleCache.contains(it.getDeviceId()))
                .toList();
        modifiableEvents.removeAll(cachedDeviceEvent);
        final var cashedDeviceEventEntities = deviceEventMapper.toEntityList(cachedDeviceEvent);
        deviceEventDataService.saveAll(cashedDeviceEventEntities);
        return modifiableEvents;
    }

}
