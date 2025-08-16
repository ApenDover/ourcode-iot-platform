package ts.andrey.eventcollector.configuration.healthcheck;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@RequiredArgsConstructor
public class KafkaHealthIndicator implements HealthIndicator {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceTopic;

    @Value("${spring.kafka.template.events-topic}")
    private String eventsTopic;

    @Override
    public Health health() {
        try {
            kafkaTemplate.send(deviceTopic, "ping").get(5, TimeUnit.SECONDS);
            kafkaTemplate.send(eventsTopic, "ping").get(5, TimeUnit.SECONDS);
            return Health.up().build();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
