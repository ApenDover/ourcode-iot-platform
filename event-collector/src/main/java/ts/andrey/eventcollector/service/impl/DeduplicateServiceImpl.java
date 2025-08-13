package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.cassandra.dao.DeviceEventDataService;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.component.SimpleCache;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeduplicateServiceImpl implements DeduplicateService {

    private final DeviceEventDataService deviceEventDataService;
    private final SimpleCache simpleCache;

    public List<DeviceEvent> getUniqDeviceEvents(List<DeviceEvent> deviceEvents) {
        return deviceEvents.stream()
                .filter(it -> !simpleCache.contains(it.getDeviceId()))
                .filter(it -> !deviceEventDataService.isExistDeviceId(it.getDeviceId()))
                .toList();
    }

}
