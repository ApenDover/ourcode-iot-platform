package ts.andrey.iotcommon.service.impl;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEventError;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ts.andrey.iotcommon.service.AbstractKafkaProducer;
import ts.andrey.iotcommon.service.KafkaProducer;
import ts.andrey.iotcommon.metrics.GlobalKafkaMetrics;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaDltProducerImpl extends AbstractKafkaProducer implements KafkaProducer {

    private final String deviceEventDlt;
    private final String deviceDlt;

    public KafkaDltProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate,
                                GlobalKafkaMetrics globalKafkaMetrics,
                                @Qualifier("deviceDlt") NewTopic deviceDlt,
                                @Qualifier("deviceEventDlt") NewTopic deviceEventDlt) {
        super(kafkaTemplate, globalKafkaMetrics, deviceDlt, deviceEventDlt);
        this.deviceEventDlt = deviceEventDlt.name();
        this.deviceDlt = deviceDlt.name();
    }

    @Override
    public CompletableFuture<List<RecordMetadata>> send(List<? extends SpecificRecordBase> records) {
        final var recordsToSend = records.stream()
                .map(recordBase -> {
                    if (recordBase instanceof DeviceEventError event) {
                        return new ProducerRecord<>(deviceEventDlt, event.getFailedEvent().getDevice().getDeviceId(), event);
                    }
                    if (recordBase instanceof DeviceError device) {
                        return new ProducerRecord<>(deviceDlt, device.getFailedEvent().getDeviceId(), device);
                    }
                    log.warn("Unsupported record type: {}", recordBase.getClass());
                    return null;
                })
                .filter(Objects::nonNull)
                .toList();
        return baseSend(recordsToSend, true);
    }

}
