package ts.andrey.devicecollector.testutils;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import lombok.SneakyThrows;
import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Future;

@Slf4j
@UtilityClass
public class KafkaProducerUtil {

    private static final String BASE_URL = "http://localhost:";

    @SneakyThrows
    public <T extends SpecificRecord> Future<RecordMetadata> sendMessage(String bootstrapServers, String topic,
                                                                         Integer schemaRegistryPort, T message) {
        Properties producerProps = new Properties();
        producerProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        producerProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        producerProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class);
        producerProps.put("schema.registry.url", BASE_URL + schemaRegistryPort);
        producerProps.put("specific.avro.reader", "true");
        producerProps.put("avro.remove.java.properties", "true");

        try (AdminClient adminClient = AdminClient.create(producerProps)) {
            Set<String> existingTopics = adminClient.listTopics().names().get();
            if (!existingTopics.contains(topic)) {
                final var newTopic = new NewTopic(topic, 1, (short) 1);
                final var created = adminClient.createTopics(Collections.singleton(newTopic)).all();
                created.get();
                log.info("Топик " + topic + " создан");
            }
        }

        try (KafkaProducer<String, T> producer = new KafkaProducer<>(producerProps)) {
            final var record = new ProducerRecord<>(topic, UUID.randomUUID().toString(), message);
            final var result = producer.send(record);
            result.get();
            return result;
        }
    }

}
