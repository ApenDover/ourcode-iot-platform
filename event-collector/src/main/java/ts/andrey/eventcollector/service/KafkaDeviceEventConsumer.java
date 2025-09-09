package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ts.andrey.eventcollector.service.component.CollectorFacade;
import ts.andrey.eventcollector.utils.TraceUtil;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceEventConsumer {

    private final CollectorFacade collectorFacade;

    @KafkaListener(
            topics = "${spring.kafka.template.events.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaBatchDeviceEventsListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceEvent> records) {
        final var trace = TraceUtil.getTraceParentFromIterator(records.iterator().next().headers());
        TraceUtil.withRootSpan(trace, () -> {
            final var events = new ArrayList<DeviceEvent>();
            records.forEach(message -> events.add(message.value()));
            log.info("Получена пачка из [{}] событий", events.size());
            log.debug("Получены события: [{}]", events);
            collectorFacade.collect(events);
        });
    }

}
