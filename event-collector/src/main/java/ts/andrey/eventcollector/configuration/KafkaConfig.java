package ts.andrey.eventcollector.configuration;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@EnableKafka
public class KafkaConfig {

    private static final Integer DEFAULT_NUM_PARTITIONS = 3;
    private static final Integer DLT_DEFAULT_NUM_PARTITIONS = 1;
    private static final Integer DEFAULT_REPLICAS = 1;

    @Value("${spring.kafka.template.events-topic}")
    private String eventsTopic;

    @Value("${spring.kafka.template.dlt-events-topic}")
    private String dltEventsTopic;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceIdTopic;

    @Value("${spring.kafka.template.dlt-device-topic}")
    private String dltDeviceIdTopic;

    @Value("${spring.application.name}")
    private String appName;

    @Bean
    public NewTopic deviceEventsTopic() {
        return TopicBuilder.name(eventsTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceEventsDlt() {
        return TopicBuilder.name(dltEventsTopic)
                .partitions(DLT_DEFAULT_NUM_PARTITIONS)
                .build();
    }

    @Bean
    public NewTopic deviceIdTopic() {
        return TopicBuilder.name(deviceIdTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceIdDlt() {
        return TopicBuilder.name(dltDeviceIdTopic)
                .partitions(DLT_DEFAULT_NUM_PARTITIONS)
                .build();
    }

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
