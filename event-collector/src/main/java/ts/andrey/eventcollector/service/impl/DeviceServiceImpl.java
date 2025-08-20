package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.annotation.WithSpan;
import ts.andrey.eventcollector.service.DeduplicateService;
import ts.andrey.eventcollector.service.DeviceService;
import ts.andrey.eventcollector.service.kafka.KafkaProducer;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DeviceServiceImpl implements DeviceService {

    private final DeduplicateService deduplicateService;
    private final KafkaProducer kafkaDeviceProducerImpl;
    private final Tracer tracer;

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
        kafkaDeviceProducerImpl.send(uniqueDevices);
    }

}
