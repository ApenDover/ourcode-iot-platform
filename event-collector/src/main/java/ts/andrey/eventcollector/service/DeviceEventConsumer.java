package ts.andrey.eventcollector.service;

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

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventConsumer {

    private final CollectorServiceImpl collectorService;

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0))
    @KafkaListener(
            topics = "${spring.kafka.template.events-topic}",
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void handleEvent(
            DeviceEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition
    ) {
        log.info("Processing event:eventId={}, deviceId={}, message={}, partition={}",
                event.getEventId(), event.getDeviceId(), event.getPayload(), partition);
        collectorService.process(event);
    }

    @DltHandler
    public void handleDlt(DeviceEvent event, @Header(KafkaHeaders.EXCEPTION_MESSAGE) String ex) {
        log.error("Event failed after retries: deviceId={}, error={}", event.getDeviceId(), ex);
    }

}
