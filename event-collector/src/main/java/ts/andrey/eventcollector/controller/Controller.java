package ts.andrey.eventcollector.controller;

import com.nashkod.avro.DeviceEvent;
import com.nashkod.avro.EventType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;
import ts.andrey.eventcollector.service.DeviceEventProducer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Controller {

    private final DeviceEventProducer deviceEventProducerImpl;

    @Value("${app.inter-endpoint.enabled:false}")
    private Boolean interEndpointEnabled;

    @PostMapping("/api/kafka/send")
    public ResponseEntity<String> sendKafkaMessage(@RequestBody String message) throws NoHandlerFoundException {
        if (!interEndpointEnabled) {
            throw new NoHandlerFoundException("POST", "/api/kafka/send", new HttpHeaders());
        }
        final var event = new DeviceEvent(
                "one",
                "device_one",
                Timestamp.from(Instant.now()).getTime(),
                EventType.TEMPERATURE,
                message);
        deviceEventProducerImpl.sendEvents(List.of(event));
        return ResponseEntity.ok("Ok");
    }

}
