package ts.andrey.eventcollector.controller;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.eventcollector.service.DeviceEventProducer;

import java.sql.Timestamp;
import java.time.Instant;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Controller {

    private final DeviceEventProducer deviceEventProducerImpl;

    @PostMapping("/api/kafka/send")
    public ResponseEntity<String> sendKafkaMessage(@RequestBody String message) {
        final var event = new DeviceEvent(
                "one",
                "device_one",
                Timestamp.from(Instant.now()).getTime(),
                EventType.TEMPERATURE,
                message);
        deviceEventProducerImpl.sendEvent(event);
        return ResponseEntity.ok("Ok");
    }

}
