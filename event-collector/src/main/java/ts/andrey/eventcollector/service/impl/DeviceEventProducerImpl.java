package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.service.DeviceEventProducer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceEventProducerImpl implements DeviceEventProducer {

    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;

    @Value("${spring.kafka.template.events-topic}")
    private String eventsTopic;

    @Override
    public CompletableFuture<List<RecordMetadata>> sendEvents(List<? extends SpecificRecordBase> records) {
        log.info("sendEvent batch");
        try {
            if (CollectionUtils.isEmpty(records)) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            final var futures = records.stream()
                    .filter(DeviceEvent.class::isInstance)
                    .map(record -> {
                        DeviceEvent event = (DeviceEvent) record;
                        String key = UUID.randomUUID().toString();

                        return kafkaTemplate.send(eventsTopic, key, event)
                                .thenApply(SendResult::getRecordMetadata)
                                .exceptionally(ex -> {
                                    log.error("Ошибка отправки eventId={}", event.getEventId(), ex);
                                    return null;
                                });
                    }).toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList()
                    );
        } catch (Exception e) {
            log.error("Ошибка при публикации eventIds в топик {}", eventsTopic, e);
        }
        return CompletableFuture.completedFuture(null);
    }

}
