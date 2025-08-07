package ts.andrey.eventcollector.service;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventProducer {

    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;

    public CompletableFuture<RecordMetadata> sendEvent(DeviceEvent event) {
        final var uuid = UUID.randomUUID().toString();
        CompletableFuture<SendResult<String, DeviceEvent>> future = kafkaTemplate.sendDefault(uuid, event);

        return future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send event: deviceId={}", event.getDeviceId(), ex);
            } else {
                log.info("Event sent: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        }).thenApply(SendResult::getRecordMetadata);
    }

}
