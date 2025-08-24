package ts.andrey.eventcollector.configuration;

import com.nashkod.avro.DeviceEvent;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Tracer;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.consumer.ConsumerRecord;
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
import ts.andrey.eventcollector.metrics.GlobalMetrics;

import java.util.function.BiFunction;

@Slf4j
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
    public NewTopic eventTopic() {
        return TopicBuilder.name(eventsTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic eventDlt() {
        return TopicBuilder.name(dltEventsTopic)
                .partitions(DLT_DEFAULT_NUM_PARTITIONS)
                .build();
    }

    @Bean
    public NewTopic deviceTopic() {
        return TopicBuilder.name(deviceIdTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceDlt() {
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

    @Bean
    public DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate,
                                            GlobalMetrics globalMetrics) {
        BiFunction<ConsumerRecord<?, ?>, Exception, TopicPartition> destinationResolver =
                (record, ex) -> {
                    globalMetrics.incrementDltError();
                    log.warn("Сообщение {} ушло в DLT из-за ошибки {}", record.key(), ex.getMessage());
                    return new TopicPartition(dltEventsTopic, record.partition());
                };

        final var recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate, destinationResolver);

        var backoff = new ExponentialBackOffWithMaxRetries(3);
        backoff.setInitialInterval(1000L);
        backoff.setMultiplier(2.0);
        backoff.setMaxInterval(10000L);

        return new DefaultErrorHandler(recoverer, backoff);
    }

}
