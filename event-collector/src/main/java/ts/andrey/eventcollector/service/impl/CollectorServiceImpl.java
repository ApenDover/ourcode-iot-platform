package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ts.andrey.eventcollector.cassandra.dataService.DeviceEventDataService;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.CollectorService;
import ts.andrey.eventcollector.service.DeviceEventProducer;
import ts.andrey.eventcollector.service.component.SimpleCache;

@Slf4j
@Component
@RequiredArgsConstructor
public class CollectorServiceImpl implements CollectorService {

    private final DeviceEventDataService deviceEventDataService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final DeviceEventMapper deviceEventMapper;
    private final SimpleCache simpleCache;

    @Override
    public void collect(DeviceEvent event) {
        try {
            log.debug("Начал обрабатывать событие: {}", event);
            final var device = deviceEventMapper.toDeviceId(event);
            final var deviceId = device.getDeviceId();
            final var entity = deviceEventMapper.toEntity(event);

            if (simpleCache.contains(deviceId)) {
                log.debug("Этот девайс уже закеширован: deviceId {}", deviceId);
                deviceEventDataService.save(entity);
                return;
            }

            if (!deviceEventDataService.isExistDeviceId(deviceId)) {
                deviceIdProducerImpl.sendEvent(device);
            }

            simpleCache.put(deviceId);
            deviceEventDataService.save(entity);
        } catch (Exception e) {
            log.error("Ошибка при обработке события {}", event, e);
        }
    }

}
