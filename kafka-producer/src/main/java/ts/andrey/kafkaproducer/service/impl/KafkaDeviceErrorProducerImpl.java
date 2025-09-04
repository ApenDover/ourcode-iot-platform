package ts.andrey.kafkaproducer.service.impl;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEvent;
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
public class KafkaDeviceErrorProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, DeviceError> kafkaTemplate;

    @Value("${spring.kafka.template.dlt-device-topic}")
    private String deviceDltTopic;

    @Override
    @WithSpan("publish-batch-event")
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        log.info("Отправка в топик [{}] новых device errors: [{}]", deviceDltTopic, records.size());
        try {
            if (CollectionUtils.isEmpty(records)) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            final var futures = records.stream()
                    .filter(DeviceError.class::isInstance)
                    .map(it -> {
                        final var event = (DeviceError) it;
                        return sendMessage(event);
                    }).toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList()
                    );
        } catch (Exception e) {
            log.error("Ошибка при публикации deviceError в топик [{}]", deviceDltTopic, e);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    public CompletableFuture<RecordMetadata> sendMessage(DeviceError event) {
        return kafkaTemplate.send(deviceDltTopic, event.getFailedEvent().getDeviceId(), event)
                .thenApply(SendResult::getRecordMetadata);
    }

}
