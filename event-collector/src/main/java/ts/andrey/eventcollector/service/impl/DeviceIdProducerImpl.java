package ts.andrey.eventcollector.service.impl;

import com.nashkod.avro.DeviceId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.exception.ExceptionMessage;
import ts.andrey.eventcollector.exception.IotException;
import ts.andrey.eventcollector.service.DeviceEventProducer;

import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class DeviceIdProducerImpl implements DeviceEventProducer {

    private final KafkaTemplate<String, DeviceId> kafkaTemplate;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceIdTopic;

    @Override
    public CompletableFuture<RecordMetadata> sendEvent(SpecificRecordBase record) {
        if (Objects.isNull(record) || !(record instanceof DeviceId deviceId)) {
            throw new IotException(ExceptionMessage.UNRECOGNIZED_RECORD_TYPE.getValue());
        }
        final var uuid = UUID.randomUUID().toString();
        final var future = kafkaTemplate.send(deviceIdTopic, uuid, deviceId);

        return future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to send deviceId: deviceId={}", deviceId.getDeviceId(), ex);
            } else {
                log.info("Event sent: partition={}, offset={}",
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        }).thenApply(SendResult::getRecordMetadata);
    }

}
