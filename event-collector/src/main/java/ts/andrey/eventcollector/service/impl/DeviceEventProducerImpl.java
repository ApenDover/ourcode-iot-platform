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
import ts.andrey.eventcollector.exception.ExceptionMessage;
import ts.andrey.eventcollector.exception.IotException;
import ts.andrey.eventcollector.service.DeviceEventProducer;

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
    public CompletableFuture<RecordMetadata> sendEvent(SpecificRecordBase record) {
        try {
            if (Objects.isNull(record) || !(record instanceof DeviceEvent event)) {
                throw new IotException(ExceptionMessage.UNRECOGNIZED_RECORD_TYPE.getValue());
            }
            log.debug("Отправляю Event в topic={} event: {}", eventsTopic, event);
            final var uuid = UUID.randomUUID().toString();
            final var future = kafkaTemplate.send(eventsTopic, uuid, event);

            return future.whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Ошибка при отправке в топик={}, eventId={}", eventsTopic, event.getEventId(), ex);
                } else {
                    log.info("Device успешно отправлен в kafka: topic={}, partition={}, offset={}",
                            eventsTopic,
                            result.getRecordMetadata().partition(),
                            result.getRecordMetadata().offset());
                }
            }).thenApply(SendResult::getRecordMetadata);
        } catch (Exception e) {
            log.error("Ошибка при публикации event в топик {}", eventsTopic, e);
        }
        return CompletableFuture.completedFuture(null);
    }

}
