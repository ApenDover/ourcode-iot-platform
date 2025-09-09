package ts.andrey.failedeventsprocessor.configuration;

import com.nashkod.avro.DeviceError;
import com.nashkod.avro.DeviceEventError;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConsumerErrorConfig {

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
