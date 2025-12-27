package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.DeviceService;
import ts.andrey.iotcommon.service.KafkaProducer;

import java.util.ArrayList;
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
    @WithSpan("deduplicate-save-send-process")
    public void sendUniqueDeviceids(List<DeviceEvent> deviceEvents) {
        final var devices = deviceEvents.stream()
                .map(DeviceEvent::getDevice)
                .toList();

        final var uniqueDevices = deduplicateService.getUniqueDevices(devices);

        final var notUniqueTechDevices = deviceEvents.stream()
                .filter(event -> com.nashkod.avro.EventType.TECH.equals(event.getType()))
                .map(DeviceEvent::getDevice)
                .filter(device -> !uniqueDevices.contains(device))
                .toList();

        final var readyToSendDevices = new ArrayList<Device>();
        readyToSendDevices.addAll(notUniqueTechDevices);
        readyToSendDevices.addAll(uniqueDevices);

        kafkaProducerImpl.send(readyToSendDevices);
    }

}
