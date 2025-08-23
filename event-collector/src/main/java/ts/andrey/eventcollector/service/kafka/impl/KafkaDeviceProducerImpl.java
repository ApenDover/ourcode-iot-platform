package ts.andrey.eventcollector.service.kafka.impl;

import com.nashkod.avro.Device;
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
import ts.andrey.eventcollector.service.kafka.KafkaProducer;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaDeviceProducerImpl extends AbstractKafkaProducer implements KafkaProducer {

    private final String deviceTopic;

    protected KafkaDeviceProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                                      GlobalMetrics globalMetrics, NewTopic deviceTopic, NewTopic deviceDlt) {
        super(kafkaTemplate, globalMetrics, deviceDlt.name());
        this.deviceTopic = deviceTopic.name();
    }

    @WithSpan("kafkaDeviceProducer")
    @Override
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        final var recordsToSend = records.stream()
                .filter(Device.class::isInstance)
                .map(recordBase -> {
                    final var device = (Device) recordBase;
                    return new ProducerRecord<>(deviceTopic, device.getDeviceId(), device);
                })
                .toList();
        return innerSend(recordsToSend);
    }

}
