package ts.andrey.devicecollector.service;

import com.nashkod.avro.Device;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ts.andrey.devicecollector.service.impl.DeviceServiceImpl;
import ts.andrey.devicecollector.utils.TraceUtil;

import java.util.ArrayList;

@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaDeviceConsumer {

    private final DeviceServiceImpl deviceService;

    @KafkaListener(
            topics = "${spring.kafka.template.device.topic}",
            groupId = "${spring.kafka.consumer.group-id}",
            batch = "true",
            containerFactory = "kafkaBatchDeviceListenerContainerFactory"
    )
    public void handleEvents(ConsumerRecords<String, Device> records) {
        final var trace = TraceUtil.getTraceParentFromIterator(records.iterator().next().headers());
        TraceUtil.withRootSpan(trace, () -> {
            final var devices = new ArrayList<Device>();
            records.forEach(message -> devices.add(message.value()));
            log.info("Получена пачка из [{}] девайсов", devices.size());
            log.debug("Получены девайсы: [{}]", devices);
            deviceService.createOrUpdateDevice(devices);
        });
    }

}
