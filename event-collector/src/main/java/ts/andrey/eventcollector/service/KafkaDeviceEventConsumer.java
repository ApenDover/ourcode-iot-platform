package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ts.andrey.eventcollector.metrics.EventCollectorMetrics;
import ts.andrey.eventcollector.service.component.CollectorFacade;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceEventConsumer {

    private final CollectorFacade collectorFacade;
    private final EventCollectorMetrics eventCollectorMetrics;

    @KafkaListener(
            topics = "${spring.kafka.template.events.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaBatchDeviceEventsListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceEvent> records) {
        final var start = System.nanoTime();
        try {
            final var events = new ArrayList<DeviceEvent>();
            records.forEach(message -> events.add(message.value()));
            log.info("Получена пачка из [{}] событий", events.size());
            log.debug("Получены события: [{}]", events);
            collectorFacade.collect(events);
        } finally {
            final var durationNs = System.nanoTime() - start;
            eventCollectorMetrics.recordTime(durationNs);
        }
    }

}
