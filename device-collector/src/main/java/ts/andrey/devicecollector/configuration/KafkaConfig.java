package ts.andrey.devicecollector.configuration;

import com.nashkod.avro.Device;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecordBase;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.TopicPartition;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.kafka.support.ExponentialBackOffWithMaxRetries;
import ts.andrey.devicecollector.metrics.GlobalMetrics;
import ts.andrey.devicecollector.utils.MessageDltBuilder;

@Configuration
@EnableKafka
@Slf4j
public class KafkaConfig {

    private static final Integer DEFAULT_NUM_PARTITIONS = 3;
    private static final Integer DEFAULT_REPLICAS = 1;

    @Value("${spring.kafka.template.device-topic}")
    private String deviceTopic;

    @Value("${spring.kafka.template.dlt-device-topic}")
    private String dltDeviceTopic;

    @Bean
    public NewTopic deviceTopic() {
        return TopicBuilder.name(deviceTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceDlt() {
        return TopicBuilder.name(dltDeviceTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Device> kafkaBatchListenerContainerFactory(
            ConsumerFactory<String, Device> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Device>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                            GlobalMetrics globalMetrics) {

        final var recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (consumerRecord, exception) ->
                        new TopicPartition(dltDeviceTopic, consumerRecord.partition())
        ) {
            @Override
            public void accept(ConsumerRecord<?, ?> record, Exception exception) {
                globalMetrics.incrementDltMessage();
                log.warn("Сообщение [{}] ушло в DLT из-за ошибки [{}]", record.key(), exception.getMessage());
                final var errorMessage = MessageDltBuilder.getMessage((SpecificRecordBase) record.value(), exception);
                kafkaTemplate.send(new ProducerRecord<>(
                        dltDeviceTopic,
                        record.partition(),
                        record.key(),
                        errorMessage,
                        record.headers()
                ));
            }
        };

        final var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);

        return new DefaultErrorHandler(recoverer, backoff);
    }

}
