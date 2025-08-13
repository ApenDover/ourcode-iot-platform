package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.CollectorService;
import ts.andrey.eventcollector.service.DeviceEventProducer;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final DeviceEventService deviceEventService;
    private final DeviceEventMapper deviceEventMapper;
    private final SimpleCache simpleCache;

    @Override
    public void collect(List<DeviceEvent> events) {
        try {
            final var uncachedDeviceEvent = deviceEventService.saveCashedEvents(events);
            final var uncachedDevices = deviceEventMapper.toDeviceIdList(uncachedDeviceEvent);
            final var unsavedDevices = uncachedDevices.stream()
                    .filter(deviceEvent -> !deviceEventDataService.isExistDeviceId(deviceEvent.getDeviceId()))
                    .toList();
            if (!CollectionUtils.isEmpty(unsavedDevices)) {
                deviceIdProducerImpl.send(unsavedDevices);
            }
            final var deviceIds = unsavedDevices.stream()
                    .map(Device::getDeviceId)
                    .toList();
            simpleCache.putAll(deviceIds);
            final var uncachedDeviceEventEntities = deviceEventMapper.toEntityList(uncachedDeviceEvent);
            deviceEventDataService.saveAll(uncachedDeviceEventEntities);
        } catch (Exception e) {
            log.error("Ошибка при обработке событий (batch)", e);
        }
    }

}
