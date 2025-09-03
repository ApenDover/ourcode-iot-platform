package ts.andrey.failedeventsprocessor.service;

import com.nashkod.avro.DeviceEventError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceEventsErrorConsumer {

    private final ErrorHandlerService errorHandlerService;

    @KafkaListener(
            topics = "${spring.kafka.template.dlt-events-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaBatchEventsErrorListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceEventError> records) {
        final var eventsErrors = new ArrayList<DeviceEventError>();
        records.forEach(message -> eventsErrors.add(message.value()));
        log.info("Получена пачка из [{}] ошибок по Events", eventsErrors.size());
        log.debug("Получены ошибки по Events: [{}]", eventsErrors);
        eventsErrors.forEach(errorHandlerService::start);
    }

}
