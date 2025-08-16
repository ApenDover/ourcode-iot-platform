package ts.andrey.eventcollector.controller;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.NoHandlerFoundException;
import ts.andrey.eventcollector.service.DeviceProducer;

import java.util.List;

@Slf4j
@RestController
@RequiredArgsConstructor
public class Controller {

    private final DeviceProducer deviceEventProducerImpl;

    @Value("${app.inter-endpoint.enabled:false}")
    private boolean interEndpointEnabled;

    @PostMapping("/api/kafka/send")
    public ResponseEntity<String> sendKafkaMessage(
            @RequestBody List<DeviceEvent> request) throws NoHandlerFoundException {
        if (!interEndpointEnabled) {
            log.info("Inter endpoint not enabled");
            throw new NoHandlerFoundException("POST", "/api/kafka/send", new HttpHeaders());
        }
        deviceEventProducerImpl.send(request);
        return ResponseEntity.ok("Ok");
    }

}
