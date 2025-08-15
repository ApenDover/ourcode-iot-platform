package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.mapper.DeviceEventMapper;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.DeviceEventProducer;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeduplicateService deduplicateService;
    private final DeviceEventProducer deviceIdProducerImpl;
    private final DeviceEventMapper deviceEventMapper;

    /**
     * Отправляем в топик новые deviceId
     *
     * @param deviceEvents список событий
     */
    public void sendUniqueDeviceids(List<DeviceEvent> deviceEvents) {
        final var devices = deviceEventMapper.toDeviceIdList(deviceEvents);
        final var uniqueDevices = deduplicateService.getUniqueDevices(devices);
        deviceIdProducerImpl.send(uniqueDevices);
    }

}
