package ts.andrey.failedeventsprocessor.configuration;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEventError;
import lombok.extern.slf4j.Slf4j;
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
@Slf4j
public class KafkaConfig {

    private static final Integer DEFAULT_NUM_PARTITIONS = 3;
    private static final Integer DEFAULT_REPLICAS = 1;

    @Value("${spring.kafka.template.dlt-device-topic}")
    private String dltDeviceTopic;

    @Value("${spring.kafka.template.dlt-events-topic}")
    private String dltEventsTopic;

    @Bean
    public NewTopic deviceDlt() {
        return TopicBuilder.name(dltDeviceTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public NewTopic eventsDlt() {
        return TopicBuilder.name(dltEventsTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceError> kafkaBatchDeviceErrorListenerContainerFactory(
            ConsumerFactory<String, DeviceError> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, DeviceError>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceEventError>
    kafkaBatchEventsErrorListenerContainerFactory(ConsumerFactory<String, DeviceEventError> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, DeviceEventError>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

}
