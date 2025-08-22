package ts.andrey.eventcollector.service.kafka.impl;

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
import ts.andrey.eventcollector.exception.EventCollectorException;
import ts.andrey.eventcollector.service.kafka.KafkaProducer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;

    @Value("${spring.kafka.template.events-topic}")
    private String eventsTopic;

    @Value("${spring.kafka.template.dlt-events-topic}")
    private String dltEventsTopic;

    @Override
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        return send(records, eventsTopic);
    }

    @Override
    public CompletableFuture<List<RecordMetadata>> sendDlt(List<? extends SpecificRecordBase> records) {
        return send(records, dltEventsTopic);
    }

    private CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records, String topic) {
        log.info("Отправка в топик {} новых device events: {}", topic, records.size());
        if (CollectionUtils.isEmpty(records)) {
            return CompletableFuture.completedFuture(Collections.emptyList());
        }

        final var futures = records.stream()
                .filter(DeviceEvent.class::isInstance)
                .map(it -> {
                    final var event = (DeviceEvent) it;
                    String key = UUID.randomUUID().toString();

                    return kafkaTemplate.send(topic, key, event)
                            .thenApply(SendResult::getRecordMetadata)
                            .exceptionally(ex -> {
                                log.error("Ошибка отправки eventId={}", event.getEventId(), ex);
                                throw new EventCollectorException(ex);
                            });
                }).toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList()
                );
    }

}
