package ts.andrey.eventcollector.service.component;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceEventConsumer {

    private final CollectorFacade collectorFacade;

    @KafkaListener(
            topics = "${spring.kafka.template.events-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaBatchListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceEvent> records) {
        final var events = new ArrayList<DeviceEvent>();
        records.forEach(record -> events.add(record.value()));
        log.info("Получена пачка из {} событий", events.size());
        log.debug("Получены события: {}", events);
        try {
            collectorFacade.collect(events);
        } catch (Exception e) {
            log.error("Ошибка обработки пачки событий", e);
            throw e;
        }
    }

}
