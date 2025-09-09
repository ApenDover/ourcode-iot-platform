package ts.andrey.failedeventsprocessor.service.kafka;

import com.nashkod.avro.DeviceError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ts.andrey.failedeventsprocessor.service.ErrorProcessor;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class DeviceErrorConsumer {

    private final ErrorProcessor<DeviceError> errorProcessor;

    @KafkaListener(
            topics = "${spring.kafka.template.device.dlt}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaBatchDeviceErrorListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceError> records) {
        final var deviceErrors = new ArrayList<DeviceError>();
        records.forEach(message -> deviceErrors.add(message.value()));
        log.info("Получена пачка из [{}] ошибок по Device", deviceErrors.size());
        log.debug("Получены ошибки по Device: [{}]", deviceErrors);
        deviceErrors.forEach(errorProcessor::start);
    }

}
