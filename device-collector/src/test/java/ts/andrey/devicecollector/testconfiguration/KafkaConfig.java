package ts.andrey.devicecollector.testconfiguration;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.config.TopicBuilder;

public class KafkaConfig {

    @Bean
    public NewTopic deviceIdTopic() {
        return TopicBuilder.name("device")
                .partitions(1)
                .replicas(1)
                .compact()
                .build();
    }

}
