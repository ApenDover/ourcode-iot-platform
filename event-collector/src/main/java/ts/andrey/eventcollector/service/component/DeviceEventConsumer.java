package ts.andrey.eventcollector.service.component;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.service.CollectorService;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventConsumer {

    private final CollectorService collectorService;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(
            topics = "${spring.kafka.template.events-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleEvent(
            DeviceEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        log.info("Нашел в топике новое событие: eventId={}, deviceId={}, message={}, partition={}",
                event.getEventId(), event.getDeviceId(), event.getPayload(), partition);
        collectorService.collect(event);
    }

    @DltHandler
    public void handleDlt(DeviceEvent event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String ex) {
        log.error("Попытался несколько раз разобрать сообщение, "
                + "безуспешно: deviceId={}, error={}", event.getDeviceId(), ex);
    }

}
