package ts.andrey.kafkaproducer.service.impl;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.instrumentation.annotations.WithSpan;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.kafkaproducer.service.KafkaProducer;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaEventProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, DeviceEvent> kafkaTemplate;
    private final Tracer tracer;

    @Value("${spring.kafka.template.events-topic}")
    private String eventsTopic;

    @Override
    @WithSpan("publish-batch-event")
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
            log.info("Отправка в топик {} новых device events: {}", eventsTopic, records.size());
            try {
                if (CollectionUtils.isEmpty(records)) {
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }

                final var futures = records.stream()
                        .filter(DeviceEvent.class::isInstance)
                        .map(it -> {
                            final var event = (DeviceEvent) it;
                            return sendMessage(event);
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
            return CompletableFuture.completedFuture(Collections.emptyList());
    }

    public CompletableFuture<RecordMetadata> sendMessage(DeviceEvent event) {
        return kafkaTemplate.send(eventsTopic, String.valueOf(event.getDevice().getDeviceId()), event)
                .thenApply(SendResult::getRecordMetadata)
                .exceptionally(ex -> {
                    log.error("Ошибка отправки eventId={}", event.getEventId(), ex);
                    return null;
                });
    }

}
