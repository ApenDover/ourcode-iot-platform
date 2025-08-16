package ts.andrey.devicecollector.configuration.healthcheck;

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
    private String topic;

    @Override
    public Health health() {
        try {
            kafkaTemplate.send(topic, "ping").get(5, TimeUnit.SECONDS);
            return Health.up().build();
        } catch (ExecutionException | InterruptedException | TimeoutException e) {
            return Health.down().withDetail("error", e.getMessage()).build();
        }
    }
}
