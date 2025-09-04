package ts.andrey.kafkaproducer.service.impl;

import com.nashkod.avro.DeviceEventError;
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
public class KafkaEventsErrorProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, DeviceEventError> kafkaTemplate;

    @Value("${spring.kafka.template.dlt-events-topic}")
    private String eventsDltTopic;

    @Override
    @WithSpan("publish-batch-event")
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        log.info("Отправка в топик [{}] новых events errors: [{}]", eventsDltTopic, records.size());
        try {
            if (CollectionUtils.isEmpty(records)) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            final var futures = records.stream()
                    .filter(DeviceEventError.class::isInstance)
                    .map(it -> {
                        final var event = (DeviceEventError) it;
                        return sendMessage(event);
                    }).toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList()
                    );
        } catch (Exception e) {
            log.error("Ошибка при публикации eventIds в топик [{}]", eventsDltTopic, e);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    public CompletableFuture<RecordMetadata> sendMessage(DeviceEventError event) {
        return kafkaTemplate.send(eventsDltTopic, event.getFailedEvent().getDevice().getDeviceId(), event)
                .thenApply(SendResult::getRecordMetadata);
    }

}
