package ts.andrey.eventcollector.service.kafka.impl;

import com.nashkod.avro.Device;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;
import ts.andrey.eventcollector.annotation.WithSpan;
import ts.andrey.eventcollector.service.kafka.KafkaProducer;
import ts.andrey.eventcollector.utils.TraceUtil;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaDeviceProducerImpl implements KafkaProducer {

    private final KafkaTemplate<String, Device> kafkaTemplate;
    private final Tracer tracer;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceTopic;

    @Override
    @WithSpan("kafkaDeviceProducer")
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        log.info("Отправка в топик {} новых device: {}", deviceTopic, records.size());
        try {
            if (CollectionUtils.isEmpty(records)) {
                return CompletableFuture.completedFuture(Collections.emptyList());
            }

            final var futures = records.stream()
                    .filter(Device.class::isInstance)
                    .map(kafkaMessage -> sendToTopic((Device) kafkaMessage))
                    .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenApply(v -> futures.stream()
                            .map(CompletableFuture::join)
                            .filter(Objects::nonNull)
                            .toList()
                    );
        } catch (Exception e) {
            log.error("Ошибка при публикации deviceId в топик {}", deviceTopic, e);
        }
        return CompletableFuture.completedFuture(Collections.emptyList());
    }

    private CompletableFuture<RecordMetadata> sendToTopic(Device device) {
        return TraceUtil.withSpan(tracer, "send-device-" + device.getDeviceId(), () -> {
            log.debug("отправляю в топик {} device={}", deviceTopic, device);

            final var trace = Span.current().getSpanContext();
            final var header = new RecordHeader("traceparent", trace.getTraceIdBytes());

            final var producerRecord = new ProducerRecord<String, Device>(
                    deviceTopic, 1, device.getDeviceId(), device, List.of(header));

            return kafkaTemplate.send(producerRecord)
                    .thenApply(SendResult::getRecordMetadata)
                    .exceptionally(ex -> {
                        log.error("Ошибка отправки device={}", device, ex);
                        return null;
                    });
        });
    }

}
