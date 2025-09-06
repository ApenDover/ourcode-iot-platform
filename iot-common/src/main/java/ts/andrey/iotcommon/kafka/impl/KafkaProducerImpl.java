package ts.andrey.iotcommon.kafka.impl;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ts.andrey.iotcommon.kafka.AbstractKafkaProducer;
import ts.andrey.iotcommon.kafka.KafkaProducer;
import ts.andrey.iotcommon.metrics.GlobalKafkaMetrics;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaProducerImpl extends AbstractKafkaProducer implements KafkaProducer {

    private final String eventTopic;
    private final String deviceTopic;

    public KafkaProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                             GlobalKafkaMetrics globalKafkaMetrics,
                             NewTopic deviceDltTopic,
                             NewTopic eventDltTopic,
                             NewTopic deviceTopic,
                             NewTopic eventTopic) {
        super(kafkaTemplate, globalKafkaMetrics, deviceDltTopic, eventDltTopic);
        this.eventTopic = eventTopic.name();
        this.deviceTopic = deviceTopic.name();
    }

    @Override
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        final var recordsToSend = records.stream()
                .map(recordBase -> {
                    if (recordBase instanceof DeviceEvent event) {
                        return new ProducerRecord<>(eventTopic, event.getDevice().getDeviceId(), event);
                    }
                    if (recordBase instanceof Device device) {
                        return new ProducerRecord<>(deviceTopic, device.getDeviceId(), device);
                    }
                    log.warn("Unsupported record type: {}", recordBase.getClass());
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        return baseSend(recordsToSend, false);
    }

}
