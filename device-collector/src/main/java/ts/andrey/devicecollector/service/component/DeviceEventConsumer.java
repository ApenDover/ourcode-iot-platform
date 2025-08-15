package ts.andrey.devicecollector.service.component;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ts.andrey.devicecollector.service.impl.DeviceServiceImpl;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventConsumer {

    private final DeviceServiceImpl deviceService;

    @KafkaListener(
            topics = "${spring.kafka.template.device-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, Device> records) {
        final var devices = new ArrayList<Device>();
        records.forEach(it -> devices.add(it.value()));
        log.info("Получена пачка из {} девайсов", devices.size());
        log.debug("Получены девайсы: {}", devices);
        try {
            deviceService.createOrUpdateDevice(devices);
        } catch (Exception e) {
            log.error("Ошибка обработки пачки событий", e);
            throw e;
        }
    }

}
