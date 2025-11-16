package ts.andrey.iotcommon.configuration;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import ts.andrey.iotcommon.service.KafkaProducer;

@Configuration
@RequiredArgsConstructor
public class KafkaExceptionHandler {

    private final KafkaProducer kafkaDltProducerImpl;

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Device> kafkaBatchDeviceListenerContainerFactory(
            ConsumerFactory<String, Device> consumerFactory,
            CommonErrorHandler commonErrorHandler) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Device>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceEvent> kafkaBatchDeviceEventsListenerContainerFactory(
            ConsumerFactory<String, DeviceEvent> consumerFactory,
            CommonErrorHandler commonErrorHandler) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, DeviceEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        factory.setCommonErrorHandler(commonErrorHandler);
        return factory;
    }

    @Bean
    public CommonErrorHandler commonErrorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        return new SimpleBatchErrorHandler(kafkaDltProducerImpl);
    }

}
