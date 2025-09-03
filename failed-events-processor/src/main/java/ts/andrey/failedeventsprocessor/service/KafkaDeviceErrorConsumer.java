package ts.andrey.failedeventsprocessor.service;

import com.nashkod.avro.DeviceError;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceErrorConsumer {

    private final ErrorHandlerService errorHandlerService;

    @KafkaListener(
            topics = "${spring.kafka.template.dlt-device-topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            containerFactory = "kafkaBatchDeviceErrorListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, DeviceError> records) {
        final var deviceErrors = new ArrayList<DeviceError>();
        records.forEach(message -> deviceErrors.add(message.value()));
        log.info("Получена пачка из [{}] ошибок по Device", deviceErrors.size());
        log.debug("Получены ошибки по Device: [{}]", deviceErrors);
        deviceErrors.forEach(errorHandlerService::start);
    }

}
