package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.data.dao.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeviceEventService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventServiceImpl implements DeviceEventService {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceEventMapper deviceEventMapper;
    private final SimpleCache simpleCache;

    @WithSpan("cassandra-save-events-and-cache")
    public void saveEvents(List<DeviceEvent> events) {
        final var mono = deviceEventDataService.saveAll(events);
        final var deviceIds = events.stream()
                .map(DeviceEvent::getDevice)
                .map(Device::getDeviceId)
                .toList();
        simpleCache.putAll(deviceIds);
        mono.block();
    }

}
