package ts.andrey.devicecollector.serializer;

import com.nashkod.avro.Device;
import io.confluent.kafka.schemaregistry.client.MockSchemaRegistryClient;
import io.confluent.kafka.serializers.KafkaAvroDeserializer;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.MockConsumer;
import org.apache.kafka.clients.consumer.OffsetResetStrategy;
import org.apache.kafka.common.TopicPartition;
import org.junit.jupiter.api.Test;
import ts.andrey.devicecollector.tdf.DummyTDF;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;


class AvroConsumerServiceTest {

    private static final String TOPIC = "device";

    @Test
    void shouldDeserializeDeviceEvent() {
        // GIVEN
        final var schemaRegistryClient = new MockSchemaRegistryClient();

        final var event = DummyTDF.device.getDefault();

        final var config = Map.of(
                "schema.registry.url", "mock://test",
                KafkaAvroDeserializerConfig.SPECIFIC_AVRO_READER_CONFIG, true
        );

        final var serializer = new KafkaAvroSerializer(schemaRegistryClient, config);
        final var avroBytes = serializer.serialize(TOPIC, event);

        final var deserializer = new KafkaAvroDeserializer(schemaRegistryClient, config);

        final var consumer = new MockConsumer<String, Object>(OffsetResetStrategy.EARLIEST);
        final var topicPartition = new TopicPartition(TOPIC, 0);
        consumer.assign(List.of(topicPartition));
        consumer.updateBeginningOffsets(Map.of(topicPartition, 0L));

        // WHEN
        consumer.addRecord(new ConsumerRecord<>(TOPIC, 0, 0L, null, deserializer.deserialize(TOPIC, avroBytes)));
        final var records = consumer.poll(java.time.Duration.ofMillis(100));
        final var received = (Device) records.iterator().next().value();

        // THEN
        assertEquals("deviceId", received.getDeviceId());
        assertEquals(Instant.ofEpochMilli(300L), received.getCreatedAt());
        assertEquals("deviceType", received.getDeviceType());
        assertEquals("meta", received.getMeta());
    }

}
