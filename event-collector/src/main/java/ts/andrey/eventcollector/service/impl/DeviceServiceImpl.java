package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.KafkaProducer;
import ts.andrey.eventcollector.service.DeviceService;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeduplicateService deduplicateService;
    private final KafkaProducer kafkaProducerImpl;

    /**
     * Отправляем в топик новые device
     *
     * @param deviceEvents список событий
     */
    public void sendUniqueDeviceids(List<DeviceEvent> deviceEvents) {
        final var devices = deviceEvents.stream()
                .map(DeviceEvent::getDevice)
                .toList();
        final var uniqueDevices = deduplicateService.getUniqueDevices(devices);
        kafkaProducerImpl.send(uniqueDevices);
    }

}
