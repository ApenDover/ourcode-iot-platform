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

    @Value("${spring.kafka.template.default-topic}")
    private String defaultTopic;

    @Value("${spring.kafka.template.dlt-default-topic}")
    private String dltDefaultTopic;

    @Bean
    public NewTopic deviceEventsTopic() {
        return TopicBuilder.name(defaultTopic)
                .partitions(DEFAULT_NUM_PARTITIONS)
                .replicas(DEFAULT_REPLICAS)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceEventsDlt() {
        return TopicBuilder.name(dltDefaultTopic)
                .partitions(DLT_DEFAULT_NUM_PARTITIONS)
                .build();
    }

}
