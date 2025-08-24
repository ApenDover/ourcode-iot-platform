package ts.andrey.devicecollector.service.kafka;

import com.nashkod.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
public class KafkaDeviceProducerImpl extends AbstractKafkaProducer implements KafkaProducer {

    private final String dltTopic;

    protected KafkaDeviceProducerImpl(KafkaTemplate<String, SpecificRecordBase> kafkaTemplate, NewTopic deviceDlt) {
        super(kafkaTemplate, deviceDlt);
        this.dltTopic = deviceDlt.name();
    }

    @Override
    public CompletableFuture<List<RecordMetadata>> sendDlt(List<? extends SpecificRecordBase> records) {
        log.error("Отправляю Device в DLT topic: {}", dltTopic);
        final var recordsToSend = records.stream()
                .filter(Device.class::isInstance)
                .map(recordBase -> {
                    final var device = (Device) recordBase;
                    return new ProducerRecord<>(dltTopic, device.getDeviceId(), device);
                })
                .toList();
        return innerSend(recordsToSend);
    }

}
