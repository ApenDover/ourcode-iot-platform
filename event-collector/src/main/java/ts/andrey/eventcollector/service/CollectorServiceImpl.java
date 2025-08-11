package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.DeviceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorServiceImpl {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final DeviceEventMapper deviceEventMapper;

    public void process(DeviceEvent event) {
        try {
            final var entity = deviceEventMapper.toEntity(event);
            deviceEventDataService.save(entity);
            final var deviceId = new DeviceId(event.getDeviceId());
            deviceIdProducerImpl.sendEvent(deviceId);
        } catch (Exception e) {
            log.error(e.getMessage());
        }
    }

}
