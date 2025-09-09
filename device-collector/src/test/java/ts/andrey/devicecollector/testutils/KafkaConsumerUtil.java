package ts.andrey.devicecollector.testutils;

import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import lombok.experimental.UtilityClass;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.util.Collections;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;

@UtilityClass
public class KafkaConsumerUtil {

    private static final String EARLIEST = "earliest";
    private static final String BASE_URL = "http://localhost:";

    public <T extends SpecificRecord> ConsumerRecords<String, T>
    getMessages(String bootstrapServers, String topic,
                String groupId, Integer schemaRegistryPort,
                Class<T> avroClass) {
        final var props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, EARLIEST);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, KafkaAvroDeserializer.class);
        props.put("schema.registry.url", BASE_URL + schemaRegistryPort);
        props.put("specific.avro.reader", "true");
        try (Consumer<String, T> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(Collections.singleton(topic));
            final var records = consumer.poll(Duration.ofSeconds(5));
            if (records.isEmpty()) {
                return null;
            }
            records.forEach(record -> assertInstanceOf(avroClass, record.value()));
            return records;
        }
    }

}
