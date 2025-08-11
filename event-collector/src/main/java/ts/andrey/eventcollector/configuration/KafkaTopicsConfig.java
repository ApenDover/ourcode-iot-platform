package ts.andrey.eventcollector.configuration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicsConfig {

    private static final Integer DEFAULT_NUM_PARTITIONS = 1;
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

}
