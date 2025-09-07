package ts.andrey.iotcommon.configuration;

import com.nashkod.avro.Device;
import com.nashkod.avro.DeviceEvent;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;

@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

    @Value("${spring.kafka.template.events.topic}")
    private String deviceEventsTopic;

    @Value("${spring.kafka.template.events.dlt}")
    private String dltDeviceEventsTopic;

    @Value("${spring.kafka.template.events.dlt-partitions:1}")
    private Integer dltDeviceEventsPartitions;

    @Value("${spring.kafka.template.events.partition:3}")
    private Integer deviceEventsPartition;

    @Value("${spring.kafka.template.events.replicas:1}")
    private Integer deviceEventsReplicas;

    @Value("${spring.kafka.template.device.topic}")
    private String deviceTopic;

    @Value("${spring.kafka.template.device.dlt}")
    private String deviceDlt;

    @Value("${spring.kafka.template.device.dlt-partitions:1}")
    private Integer dltDevicePartitions;

    @Value("${spring.kafka.template.device.partition:3}")
    private Integer devicePartition;

    @Value("${spring.kafka.template.device.replicas:1}")
    private Integer deviceReplicas;

    @Bean
    public NewTopic deviceEventTopic() {
        return TopicBuilder.name(deviceEventsTopic)
                .partitions(deviceEventsPartition)
                .replicas(deviceEventsReplicas)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceEventDlt() {
        return TopicBuilder.name(dltDeviceEventsTopic)
                .partitions(dltDeviceEventsPartitions)
                .build();
    }

    @Bean
    public NewTopic deviceTopic() {
        return TopicBuilder.name(deviceTopic)
                .partitions(devicePartition)
                .replicas(deviceReplicas)
                .compact()
                .build();
    }

    @Bean
    public NewTopic deviceDlt() {
        return TopicBuilder.name(deviceDlt)
                .partitions(dltDevicePartitions)
                .build();
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, Device> kafkaBatchDeviceListenerContainerFactory(
            ConsumerFactory<String, Device> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, Device>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, DeviceEvent> kafkaBatchDeviceEventsListenerContainerFactory(
            ConsumerFactory<String, DeviceEvent> consumerFactory) {
        final var factory = new ConcurrentKafkaListenerContainerFactory<String, DeviceEvent>();
        factory.setConsumerFactory(consumerFactory);
        factory.setBatchListener(true);
        return factory;
    }

}
