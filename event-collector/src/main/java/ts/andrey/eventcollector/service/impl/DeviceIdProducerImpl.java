package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.Device;
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
public class DeviceIdProducerImpl implements DeviceEventProducer {

    private final KafkaTemplate<String, Device> kafkaTemplate;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceIdTopic;

    @Override
    public CompletableFuture<List<RecordMetadata>> sendEvents(List<? extends SpecificRecordBase> records) {
        log.info("send device Batch");
        try {
            if (CollectionUtils.isEmpty(records)) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            final var futures = records.stream()
                    .filter(Device.class::isInstance)
                    .map(record -> {
                        Device device = (Device) record;
                        String key = UUID.randomUUID().toString();

                        return kafkaTemplate.send(deviceIdTopic, key, device)
                                .thenApply(SendResult::getRecordMetadata)
                                .exceptionally(ex -> {
                                    log.error("Ошибка отправки deviceId={}", device.getDeviceId(), ex);
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
            log.error("Ошибка при публикации deviceId в топик {}", deviceIdTopic, e);
        }
        return CompletableFuture.completedFuture(null);
    }

}
