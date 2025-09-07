package ts.andrey.eventcollector.configuration;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import ts.andrey.iotcommon.service.KafkaProducer;

@Slf4j
@Configuration
@RequiredArgsConstructor
@EnableKafka
public class KafkaConfig {

    private final KafkaProducer kafkaDltProducerImpl;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceEvent> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, DeviceEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, DeviceEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public Tracer tracer(OpenTelemetry openTelemetry) {
        return openTelemetry.getTracer(appName, "1.0.0");
    }

}
