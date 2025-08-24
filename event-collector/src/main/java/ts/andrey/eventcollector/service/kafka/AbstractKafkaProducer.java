package ts.andrey.eventcollector.service.kafka;

import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import ts.andrey.eventcollector.metrics.GlobalMetrics;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
public abstract class AbstractKafkaProducer {

    private final KafkaTemplate<String, SpecificRecordBase> kafkaTemplate;
    private final GlobalMetrics globalMetrics;
    private final String dltTopic;

    protected AbstractKafkaProducer(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                                    GlobalMetrics globalMetrics, String dltTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.globalMetrics = globalMetrics;
        this.dltTopic = dltTopic;
    }

    protected CompletableFuture<List<RecordMetadata>> innerSend(
            List<? extends ProducerRecord<String, ? extends SpecificRecordBase>> records) {
        log.info("Отправка в kafka записей={}", records.size());
        List<CompletableFuture<RecordMetadata>> futures = records.stream()
                .map(message -> kafkaTemplate.send(
                                new ProducerRecord<>(message.topic(), message.key(), message.value())
                        ).thenApply(SendResult::getRecordMetadata)
                        .exceptionallyCompose(ex -> {
                            globalMetrics.incrementError();
                            log.error("Ошибка отправки записи={}", message.value(), ex);
                            return kafkaTemplate.send(
                                    new ProducerRecord<>(dltTopic, message.key(), message.value())
                            ).thenApply(SendResult::getRecordMetadata);
                        }))
                .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> futures.stream()
                        .map(CompletableFuture::join)
                        .toList());
    }

}
