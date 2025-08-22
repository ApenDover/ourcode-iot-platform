package ts.andrey.kafkaproducer.controller;

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
import ts.andrey.kafkaproducer.model.GenerateResponse;
import ts.andrey.kafkaproducer.service.KafkaProducer;
import ts.andrey.kafkaproducer.utils.MessageGenerator;

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
    public ResponseEntity<GenerateResponse> sendKafkaMessageGenerate(
            @RequestParam("messageCount") long messageCount,
            @RequestParam("deviceCount") long deviceCount,
            @RequestParam(name = "saveDevice", required = false, defaultValue = "false") Boolean saveDevice
    ) {
        final var messages = MessageGenerator.generate(messageCount, deviceCount, saveDevice);
        kafkaProducer.send(messages);
        final var deviceIdsCount = messages.stream()
                .map(it -> it.getDevice().getDeviceId())
                .distinct()
                .count();
        return ResponseEntity.ok(new GenerateResponse(deviceIdsCount, messageCount));
    }

}
