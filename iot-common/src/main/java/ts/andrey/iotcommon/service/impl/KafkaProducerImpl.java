package ts.andrey.iotcommon.service.impl;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import ts.andrey.iotcommon.metrics.GlobalKafkaMetrics;
import ts.andrey.iotcommon.service.AbstractKafkaProducer;
import ts.andrey.iotcommon.service.KafkaProducer;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Component
public class KafkaProducerImpl extends AbstractKafkaProducer implements KafkaProducer {

    private final String eventTopic;
    private final String deviceTopic;

    public KafkaProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                             GlobalKafkaMetrics globalKafkaMetrics,
                             @Qualifier("deviceDlt") NewTopic deviceDlt,
                             @Qualifier("deviceEventDlt") NewTopic deviceEventDlt,
                             @Qualifier("deviceTopic") NewTopic deviceTopic,
                             @Qualifier("deviceEventTopic") NewTopic deviceEventTopic) {
        super(kafkaTemplate, globalKafkaMetrics, deviceDlt, deviceEventDlt);
        this.eventTopic = deviceEventTopic.name();
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
