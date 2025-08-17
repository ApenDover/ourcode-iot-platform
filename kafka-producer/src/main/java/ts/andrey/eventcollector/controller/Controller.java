package ts.andrey.eventcollector.controller;

import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ts.andrey.eventcollector.service.KafkaProducer;
import ts.andrey.eventcollector.utils.MessageGenerator;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/kafka")
@RequiredArgsConstructor
public class Controller {

    private final KafkaProducer kafkaProducer;

    @PostMapping("/send")
    public ResponseEntity<String> sendKafkaMessage(
            @RequestBody List<DeviceEvent> request) {
        kafkaProducer.send(request);
        return ResponseEntity.ok("Ok");
    }

    @GetMapping("/send/generate")
    public ResponseEntity<String> sendKafkaMessageGenerate(
            @RequestParam("messageCount") Integer messageCount,
            @RequestParam("deviceCount") Integer deviceCount
    ) {
        final var messages = MessageGenerator.generate(messageCount, deviceCount);
        kafkaProducer.send(messages);
        return ResponseEntity.ok("Ok");
    }

}
