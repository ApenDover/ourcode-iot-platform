package ts.andrey.iotcommon.service;

import com.nashkod.avro.DeviceEventError;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import ts.andrey.iotcommon.exception.IotException;
import ts.andrey.iotcommon.metrics.GlobalKafkaMetrics;
import ts.andrey.iotcommon.utils.MessageDltBuilder;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractKafkaProducer {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    private final GlobalKafkaMetrics globalKafkaMetrics;
    private final NewTopic deviceDlt;
    private final NewTopic deviceEventDlt;

    protected AbstractKafkaProducer(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                                    GlobalKafkaMetrics globalKafkaMetrics, NewTopic deviceDlt, NewTopic deviceEventDlt) {
        this.kafkaTemplate = kafkaTemplate;
        this.globalKafkaMetrics = globalKafkaMetrics;
        this.deviceDlt = deviceDlt;
        this.deviceEventDlt = deviceEventDlt;
    }

    protected CompletableFuture<List<RecordMetadata>> baseSend(
            List<? extends ProducerRecord<String, ? extends SpecificRecordBase>> records, Boolean isDlt) {
        if (CollectionUtils.isEmpty(records)) {
            return CompletableFuture.completedFuture(List.of());
        }
        log.info("Отправка в kafka[{}] записей [{}]", records.getFirst().topic(), records.size());
        List<CompletableFuture<RecordMetadata>> futures = records.stream()
                .map(messageRecord -> kafkaTemplate.send(
                                new ProducerRecord<>(messageRecord.topic(), messageRecord.key(), messageRecord.value())
                        ).thenApply(sendResult -> {
                            if (BooleanUtils.isTrue(isDlt)) {
                                globalKafkaMetrics.incrementDlt(messageRecord.topic());
                            }
                            if (BooleanUtils.isFalse(isDlt)) {
                                globalKafkaMetrics.incrementSuccess(messageRecord.topic());
                            }
                            return sendResult.getRecordMetadata();
                        })
                        .exceptionallyCompose(ex -> {
                            try {
                                final var errorMessage = MessageDltBuilder.getMessage(messageRecord.value(), ex);
                                if (errorMessage instanceof DeviceEventError deviceEventError) {
                                    return dltSend(new ProducerRecord<>(deviceEventDlt.name(), deviceEventError));
                                }
                                return dltSend(new ProducerRecord<>(deviceDlt.name(), errorMessage));
                            } catch (IotException e) {
                                globalKafkaMetrics.incrementErrorDlt();
                                log.error(e.getMessage(), e);
                                return CompletableFuture.completedFuture(null);
                            }
                        }))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .filter(Objects::nonNull)
                        .toList());
    }

    protected CompletableFuture<RecordMetadata> dltSend(
            ProducerRecord<String, ? extends SpecificRecordBase> errorRecord) {
        log.warn("Отправка сообщения в dlt kafka[{}]", errorRecord.topic());
        CompletableFuture<RecordMetadata> future =
                kafkaTemplate.send(
                                new ProducerRecord<>(errorRecord.topic(), errorRecord.key(), errorRecord.value())
                        ).thenApply(sendResult -> {
                            globalKafkaMetrics.incrementDlt(errorRecord.topic());
                            return sendResult.getRecordMetadata();
                        })
                        .exceptionallyCompose(ex -> {
                            log.error("Ошибка отправки в DLT записи=[{}]", errorRecord.value(), ex);
                            globalKafkaMetrics.incrementErrorDlt();
                            return CompletableFuture.completedFuture(null);
                        });

        return CompletableFuture.allOf(future)
                .thenApply(v -> future.join());
    }

}
