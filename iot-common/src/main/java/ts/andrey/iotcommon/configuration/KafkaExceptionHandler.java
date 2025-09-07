package ts.andrey.iotcommon.configuration;

import lombok.RequiredArgsConstructor;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import ts.andrey.iotcommon.metrics.GlobalKafkaMetrics;
import ts.andrey.iotcommon.service.KafkaProducer;
import ts.andrey.iotcommon.utils.MessageDltBuilder;

import java.util.List;

@Configuration
@RequiredArgsConstructor
public class KafkaExceptionHandler {

    private final KafkaProducer kafkaDltProducerImpl;

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                            GlobalKafkaMetrics globalMetrics) {

        final var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate) {
            @Override
            public void accept(ConsumerRecord<?, ?> record, Exception exception) {
                final var errorMessage = MessageDltBuilder.getMessage((SpecificRecordBase) record.value(), exception);
                kafkaDltProducerImpl.send(List.of(errorMessage));
            }
        };

        final var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);

        return new DefaultErrorHandler(recoverer, backoff);
    }

}
