package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.CollectorService;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final DeviceEventService deviceEventService;
    private final DeviceService deviceService;
    private final DeviceEventMapper deviceEventMapper;

    @Override
    public void collect(List<DeviceEvent> events) {
        if (CollectionUtils.isEmpty(events)) {
            return;
        }
        try {
            final var uncachedDeviceEvent = deviceEventService.saveCashedEvents(events);
            final var uncachedDevices = deviceEventMapper.toDeviceIdList(uncachedDeviceEvent);
            deviceService.process(uncachedDevices);
            deviceEventService.saveEvents(uncachedDeviceEvent);
        } catch (Exception e) {
            log.error("Ошибка при обработке событий", e);
        }
    }

}
