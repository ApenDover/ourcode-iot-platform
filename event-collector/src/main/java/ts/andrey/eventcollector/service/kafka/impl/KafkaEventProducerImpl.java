package ts.andrey.eventcollector.service.kafka.impl;

import com.nashkod.avro.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ts.andrey.eventcollector.annotation.WithSpan;
import ts.andrey.eventcollector.metrics.GlobalMetrics;
import ts.andrey.eventcollector.service.kafka.AbstractKafkaProducer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaEventProducerImpl extends AbstractKafkaProducer {

    private final String eventTopic;

    protected KafkaEventProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                                     GlobalMetrics globalMetrics, NewTopic eventTopic, NewTopic eventDlt) {
        super(kafkaTemplate, globalMetrics, eventDlt.name());
        this.eventTopic = eventTopic.name();
    }

    @WithSpan("kafkaDeviceProducer")
    public CompletableFuture<List<RecordMetadata>> send(List<DeviceEvent> devices) {
        final var recordsToSend = devices.stream()
                .map(event -> new ProducerRecord<>(eventTopic, event.getDevice().getDeviceId(), event))
                .toList();
        return innerSend(recordsToSend);
    }

}
